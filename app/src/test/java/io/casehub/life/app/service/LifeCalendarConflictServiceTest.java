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
import io.casehub.work.runtime.model.WorkItemEntity;
import jakarta.enterprise.event.Event;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifeCalendarConflictServiceTest {

    private InMemoryLifeCalendarMappingStore mappingStore;
    private InMemoryLifeCalendarConflictStore conflictStore;
    private EntityManager em;
    private CalendarPlatform platform;
    @SuppressWarnings("unchecked")
    private final Event<CalendarConflictResolved> resolvedEvent = mock(Event.class);
    private LifeCalendarConflictService service;

    private static final EventTiming ORIGINAL_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
    private static final EventTiming EXTERNAL_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11));

    @BeforeEach
    void setUp() {
        mappingStore = new InMemoryLifeCalendarMappingStore();
        conflictStore = new InMemoryLifeCalendarConflictStore();
        em = mock(EntityManager.class);
        platform = mock(CalendarPlatform.class);
        when(platform.id()).thenReturn("google");
        var platformService = new CalendarPlatformService(List.of(platform));
        service = new LifeCalendarConflictService(conflictStore, mappingStore, platformService, em, resolvedEvent);
    }

    @Test
    void accept_modified_updatesWorkItemAndMapping() {
        var mapping = seedMapping("gcal-1", "[HEALTH] GP visit", ORIGINAL_TIMING);
        var conflict = seedConflict(mapping.id(), CalendarConflictType.MODIFIED, "New summary", EXTERNAL_TIMING);
        var workItem = new WorkItemEntity();
        workItem.id = UUID.fromString(mapping.entityId());
        workItem.expiresAt = Instant.now();
        when(em.find(WorkItemEntity.class, workItem.id)).thenReturn(workItem);

        service.accept(conflict.id());

        var resolved = conflictStore.findById(conflict.id()).orElseThrow();
        assertThat(resolved.status()).isEqualTo(CalendarConflictStatus.ACCEPTED);
        assertThat(resolved.resolvedAt()).isNotNull();

        var externalAllDay = (EventTiming.AllDay) EXTERNAL_TIMING;
        assertThat(workItem.expiresAt).isEqualTo(externalAllDay.start().atStartOfDay(ZoneOffset.UTC).toInstant());

        var updatedMapping = mappingStore.findByCalendarEventId("gcal-1").getFirst();
        assertThat(updatedMapping.pushedTiming()).isEqualTo(EXTERNAL_TIMING);

        verify(resolvedEvent).fire(any(CalendarConflictResolved.class));
    }

    @Test
    void accept_deleted_deletesMapping() {
        var mapping = seedMapping("gcal-del", "[FINANCE] Budget", ORIGINAL_TIMING);
        var conflict = seedConflict(mapping.id(), CalendarConflictType.DELETED, null, null);

        service.accept(conflict.id());

        var resolved = conflictStore.findById(conflict.id()).orElseThrow();
        assertThat(resolved.status()).isEqualTo(CalendarConflictStatus.ACCEPTED);
        assertThat(mappingStore.findByCalendarEventId("gcal-del")).isEmpty();
    }

    @Test
    void dismiss_modified_pushesBackToCasehub() {
        var mapping = seedMapping("gcal-1", "[HEALTH] GP visit", ORIGINAL_TIMING);
        var conflict = seedConflict(mapping.id(), CalendarConflictType.MODIFIED, "External summary", EXTERNAL_TIMING);

        service.dismiss(conflict.id());

        var resolved = conflictStore.findById(conflict.id()).orElseThrow();
        assertThat(resolved.status()).isEqualTo(CalendarConflictStatus.DISMISSED);

        verify(platform).updateEvent(eq("primary"), eq("gcal-1"), any(EventDetails.class));
        verify(resolvedEvent).fire(any(CalendarConflictResolved.class));
    }

    @Test
    void dismiss_deleted_recreatesEvent() {
        var mapping = seedMapping("gcal-del", "[HEALTH] GP visit", ORIGINAL_TIMING);
        var conflict = seedConflict(mapping.id(), CalendarConflictType.DELETED, null, null);
        var newEvent = new CalendarEvent("gcal-new", "primary", "[HEALTH] GP visit", null, null, ORIGINAL_TIMING, List.of(), null);
        when(platform.createEvent(eq("primary"), any(EventDetails.class))).thenReturn(newEvent);

        service.dismiss(conflict.id());

        var resolved = conflictStore.findById(conflict.id()).orElseThrow();
        assertThat(resolved.status()).isEqualTo(CalendarConflictStatus.DISMISSED);

        verify(platform).createEvent(eq("primary"), any(EventDetails.class));

        var updatedMapping = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, mapping.entityId());
        assertThat(updatedMapping).isPresent();
        assertThat(updatedMapping.get().calendarEventId()).isEqualTo("gcal-new");
    }

    @Test
    void resolve_alreadyResolved_throws409() {
        var mapping = seedMapping("gcal-1", "Summary", ORIGINAL_TIMING);
        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                "ext", EXTERNAL_TIMING, CalendarConflictStatus.ACCEPTED,
                Instant.now(), "admin", LifeDomain.HEALTH);
        conflictStore.save(conflict);

        assertThatThrownBy(() -> service.accept(conflict.id()))
                .isInstanceOf(WebApplicationException.class)
                .hasMessageContaining("409");
    }

    private LifeCalendarMapping seedMapping(String eventId, String summary, EventTiming timing) {
        return mappingStore.save(new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", eventId,
                LifeDomain.HEALTH, summary, timing, Instant.now(), Instant.now()));
    }

    private LifeCalendarConflict seedConflict(UUID mappingId, CalendarConflictType type,
                                               String extSummary, EventTiming extTiming) {
        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mappingId, type, Instant.now(),
                extSummary, extTiming, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH);
        return conflictStore.save(conflict);
    }
}
