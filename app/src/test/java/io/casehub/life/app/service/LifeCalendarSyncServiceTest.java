package io.casehub.life.app.service;

import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.model.EventDetails;
import io.casehub.connectors.calendar.spi.CalendarPlatform;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.event.CalendarConflictResolved;
import io.casehub.life.app.repository.InMemoryLifeCalendarConflictStore;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifeCalendarSyncServiceTest {

    CalendarPlatform calendarPlatform;
    InMemoryLifeCalendarMappingStore mappingStore;
    InMemoryLifeCalendarConflictStore conflictStore;
    @SuppressWarnings("unchecked")
    Event<CalendarConflictResolved> resolvedEvent = mock(Event.class);
    LifeCalendarSyncService service;

    @BeforeEach
    void setUp() {
        calendarPlatform = mock(CalendarPlatform.class);
        var platformService = mock(CalendarPlatformService.class);
        when(platformService.platform("google")).thenReturn(calendarPlatform);
        mappingStore = new InMemoryLifeCalendarMappingStore();
        conflictStore = new InMemoryLifeCalendarConflictStore();
        service = new LifeCalendarSyncService(platformService, mappingStore, conflictStore,
                resolvedEvent, "google", "primary");
    }

    @Test
    void syncWorkItemCreatesCalendarEventAndMapping() {
        var workItemId = UUID.randomUUID();
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any(EventDetails.class))).thenReturn(createdEvent);

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);

        verify(calendarPlatform).createEvent(eq("primary"), any(EventDetails.class));
        var mapping = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString());
        assertThat(mapping).isPresent();
        assertThat(mapping.get().calendarEventId()).isEqualTo("gcal-1");
        assertThat(mapping.get().domain()).isEqualTo(LifeDomain.HEALTH);
    }

    @Test
    void syncWorkItemUpdatesExistingCalendarEvent() {
        var workItemId = UUID.randomUUID();
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(createdEvent);
        when(calendarPlatform.updateEvent(eq("primary"), eq("gcal-1"), any())).thenReturn(createdEvent);

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);
        service.syncWorkItem(workItemId, "GP check-up — rescheduled", LifeDomain.HEALTH, expiresAt);

        verify(calendarPlatform).updateEvent(eq("primary"), eq("gcal-1"), any(EventDetails.class));
    }

    @Test
    void removeWorkItemDeletesCalendarEvent() {
        var workItemId = UUID.randomUUID();
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(createdEvent);

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);
        service.removeWorkItem(workItemId);

        verify(calendarPlatform).deleteEvent("primary", "gcal-1");
        assertThat(mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString())).isEmpty();
    }

    @Test
    void listEventsEnrichesCasehubManagedEvents() {
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var workItemId = UUID.randomUUID();
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(createdEvent);
        when(calendarPlatform.listEvents(eq("primary"), any(), any())).thenReturn(List.of(createdEvent));

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);
        var events = service.listEvents(expiresAt.minusSeconds(86400), expiresAt.plusSeconds(86400));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().casehubManaged()).isTrue();
        assertThat(events.getFirst().domain()).isEqualTo(LifeDomain.HEALTH);
        assertThat(events.getFirst().entityId()).isEqualTo(workItemId.toString());
    }

    @Test
    void listEventsReturnsExternalEventsAsUnmanaged() {
        var from = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var to = LocalDate.of(2026, 11, 8).atStartOfDay(ZoneOffset.UTC).toInstant();
        var externalEvent = new CalendarEvent("ext-1", "primary", "Family dinner",
                null, "Home",
                new EventTiming.AllDay(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6)),
                List.of(), null);
        when(calendarPlatform.listEvents(eq("primary"), any(), any())).thenReturn(List.of(externalEvent));

        var events = service.listEvents(from, to);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().casehubManaged()).isFalse();
        assertThat(events.getFirst().domain()).isNull();
        assertThat(events.getFirst().entityId()).isNull();
    }

    @Test
    void inferDomainFromDescriptionParsesMetadata() {
        assertThat(LifeCalendarSyncService.inferDomainFromDescription("casehub:domain:HEALTH")).isEqualTo(LifeDomain.HEALTH);
        assertThat(LifeCalendarSyncService.inferDomainFromDescription("casehub:domain:FINANCE")).isEqualTo(LifeDomain.FINANCE);
        assertThat(LifeCalendarSyncService.inferDomainFromDescription(null)).isNull();
        assertThat(LifeCalendarSyncService.inferDomainFromDescription("no domain here")).isNull();
    }

    @Test
    void syncWorkItem_autoDismissesExistingConflict() {
        var workItemId = UUID.randomUUID();
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var timing = new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16));
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null, timing, List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(createdEvent);
        when(calendarPlatform.updateEvent(eq("primary"), eq("gcal-1"), any())).thenReturn(createdEvent);

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);

        var mapping = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString()).orElseThrow();
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                "External change", null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));

        service.syncWorkItem(workItemId, "GP check-up — updated", LifeDomain.HEALTH, expiresAt);

        assertThat(conflictStore.countUnresolved()).isZero();
        verify(resolvedEvent).fire(any(CalendarConflictResolved.class));
    }

    @Test
    void removeWorkItem_deletesConflictsBeforeMapping() {
        var workItemId = UUID.randomUUID();
        var expiresAt = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        var timing = new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16));
        var createdEvent = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null, timing, List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(createdEvent);

        service.syncWorkItem(workItemId, "GP check-up", LifeDomain.HEALTH, expiresAt);

        var mapping = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString()).orElseThrow();
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.DELETED, Instant.now(),
                null, null, CalendarConflictStatus.ACCEPTED,
                Instant.now(), "admin", LifeDomain.HEALTH));

        service.removeWorkItem(workItemId);

        assertThat(conflictStore.findByMapping(mapping.id())).isEmpty();
        assertThat(mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString())).isEmpty();
    }
}
