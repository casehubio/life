package io.casehub.life.app.service;

import io.casehub.connectors.SyncRequest;
import io.casehub.connectors.SyncResult;
import io.casehub.connectors.SyncTokenExpiredException;
import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.spi.CalendarPlatform;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarSyncState;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.event.CalendarConflictDetected;
import io.casehub.life.app.repository.InMemoryLifeCalendarConflictStore;
import io.casehub.life.app.repository.InMemoryLifeCalendarSyncStateStore;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifeCalendarInboundSyncJobTest {

    private InMemoryLifeCalendarMappingStore mappingStore;
    private InMemoryLifeCalendarConflictStore conflictStore;
    private InMemoryLifeCalendarSyncStateStore syncStateStore;
    private CalendarPlatform platform;
    private LifeCalendarInboundSyncJob syncJob;

    private static final EventTiming ORIGINAL_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
    private static final EventTiming CHANGED_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        mappingStore = new InMemoryLifeCalendarMappingStore();
        conflictStore = new InMemoryLifeCalendarConflictStore();
        syncStateStore = new InMemoryLifeCalendarSyncStateStore();
        platform = mock(CalendarPlatform.class);
        when(platform.id()).thenReturn("google");
        var platformService = new CalendarPlatformService(List.of(platform));
        Event<CalendarConflictDetected> detectedEvent = mock(Event.class);
        var detector = new LifeCalendarConflictDetector(mappingStore, conflictStore, detectedEvent);
        syncJob = new LifeCalendarInboundSyncJob(platformService, syncStateStore, detector,
                "google", "primary", 100);
    }

    @Test
    void executeSync_detectsConflict() {
        var mapping = seedMapping("gcal-1", "Original", ORIGINAL_TIMING);
        var changedEvent = new CalendarEvent("gcal-1", "primary", "Original", null, null, CHANGED_TIMING, List.of(), null);
        when(platform.listEventsSync(eq("primary"), any(SyncRequest.class)))
                .thenReturn(new SyncResult<>(List.of(changedEvent), List.of(), "new-token", false));

        int count = syncJob.executeSync();
        assertThat(count).isEqualTo(1);

        var syncState = syncStateStore.findByCalendar("google", "primary");
        assertThat(syncState).isPresent();
        assertThat(syncState.get().syncToken()).isEqualTo("new-token");
        assertThat(conflictStore.findUnresolved()).hasSize(1);
    }

    @Test
    void executeSync_usesStoredSyncToken() {
        syncStateStore.save(new LifeCalendarSyncState(
                UUID.randomUUID(), "google", "primary", "existing-token", Instant.now()));
        when(platform.listEventsSync(eq("primary"), any(SyncRequest.class)))
                .thenReturn(new SyncResult<>(List.of(), List.of(), "updated-token", false));

        syncJob.executeSync();

        var syncState = syncStateStore.findByCalendar("google", "primary");
        assertThat(syncState.get().syncToken()).isEqualTo("updated-token");
    }

    @Test
    void executeSync_handlesTokenExpiry() {
        syncStateStore.save(new LifeCalendarSyncState(
                UUID.randomUUID(), "google", "primary", "expired-token", Instant.now()));
        when(platform.listEventsSync(eq("primary"), any(SyncRequest.class)))
                .thenThrow(new SyncTokenExpiredException("expired-token", "google"))
                .thenReturn(new SyncResult<>(List.of(), List.of(), "fresh-token", false));

        int count = syncJob.executeSync();
        assertThat(count).isZero();

        var syncState = syncStateStore.findByCalendar("google", "primary");
        assertThat(syncState.get().syncToken()).isEqualTo("fresh-token");
    }

    private LifeCalendarMapping seedMapping(String eventId, String summary, EventTiming timing) {
        return mappingStore.save(new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", eventId,
                LifeDomain.HEALTH, summary, timing, Instant.now(), Instant.now()));
    }
}
