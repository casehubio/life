package io.casehub.life.app.service;

import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.event.CalendarConflictDetected;
import io.casehub.life.app.repository.InMemoryLifeCalendarConflictStore;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LifeCalendarConflictDetectorTest {

    private InMemoryLifeCalendarMappingStore mappingStore;
    private InMemoryLifeCalendarConflictStore conflictStore;
    @SuppressWarnings("unchecked")
    private final Event<CalendarConflictDetected> detectedEvent = mock(Event.class);
    private LifeCalendarConflictDetector detector;

    private static final EventTiming ORIGINAL_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
    private static final EventTiming CHANGED_TIMING =
            new EventTiming.AllDay(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6));

    @BeforeEach
    void setUp() {
        mappingStore = new InMemoryLifeCalendarMappingStore();
        conflictStore = new InMemoryLifeCalendarConflictStore();
        detector = new LifeCalendarConflictDetector(mappingStore, conflictStore, detectedEvent);
    }

    @Test
    void changedEvent_noMapping_skips() {
        var event = calendarEvent("gcal-unknown", "Summary", ORIGINAL_TIMING);
        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isZero();
        verify(detectedEvent, never()).fire(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changedEvent_matchingSummaryAndTiming_skipsEcho() {
        var mapping = seedMapping("gcal-1", "Summary", ORIGINAL_TIMING);
        var event = calendarEvent("gcal-1", "Summary", ORIGINAL_TIMING);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isZero();
    }

    @Test
    void changedEvent_differentTiming_createsModifiedConflict() {
        var mapping = seedMapping("gcal-1", "[HEALTH] GP visit", ORIGINAL_TIMING);
        var event = calendarEvent("gcal-1", "[HEALTH] GP visit", CHANGED_TIMING);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isEqualTo(1);

        var conflicts = conflictStore.findUnresolved();
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.getFirst().conflictType()).isEqualTo(CalendarConflictType.MODIFIED);
        assertThat(conflicts.getFirst().externalTiming()).isEqualTo(CHANGED_TIMING);
        assertThat(conflicts.getFirst().domain()).isEqualTo(LifeDomain.HEALTH);

        var captor = ArgumentCaptor.forClass(CalendarConflictDetected.class);
        verify(detectedEvent).fire(captor.capture());
        assertThat(captor.getValue().conflictType()).isEqualTo(CalendarConflictType.MODIFIED);
    }

    @Test
    void changedEvent_caseMilestone_skips() {
        var mappingId = UUID.randomUUID();
        mappingStore.save(new LifeCalendarMapping(
                mappingId, CalendarEntityType.CASE_MILESTONE, "case-1:milestone-a",
                "google", "primary", "gcal-milestone",
                LifeDomain.TRAVEL, "Trip milestone", ORIGINAL_TIMING, Instant.now(), Instant.now()));
        var event = calendarEvent("gcal-milestone", "Changed milestone", CHANGED_TIMING);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isZero();
    }

    @Test
    void changedEvent_nullPushedTiming_skips() {
        var mappingId = UUID.randomUUID();
        mappingStore.save(new LifeCalendarMapping(
                mappingId, CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", "gcal-pre-migration",
                LifeDomain.FINANCE, "Budget", null, Instant.now(), Instant.now()));
        var event = calendarEvent("gcal-pre-migration", "Budget changed", CHANGED_TIMING);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isZero();
    }

    @Test
    void changedEvent_existingUnresolvedWithSameValues_skipsIdempotent() {
        var mapping = seedMapping("gcal-1", "Summary", ORIGINAL_TIMING);
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                "New summary", CHANGED_TIMING, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));
        var event = calendarEvent("gcal-1", "New summary", CHANGED_TIMING);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isZero();
        assertThat(conflictStore.findUnresolved()).hasSize(1);
    }

    @Test
    void changedEvent_existingUnresolvedWithDifferentValues_updatesConflict() {
        var mapping = seedMapping("gcal-1", "Summary", ORIGINAL_TIMING);
        var existingConflictId = UUID.randomUUID();
        conflictStore.save(new LifeCalendarConflict(
                existingConflictId, mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                "Old external summary", CHANGED_TIMING, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));
        var newerTiming = new EventTiming.AllDay(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2));
        var event = calendarEvent("gcal-1", "Newer summary", newerTiming);

        int count = detector.detectConflicts(List.of(event), List.of());
        assertThat(count).isEqualTo(1);
        var updated = conflictStore.findById(existingConflictId);
        assertThat(updated).isPresent();
        assertThat(updated.get().externalSummary()).isEqualTo("Newer summary");
        assertThat(updated.get().externalTiming()).isEqualTo(newerTiming);
    }

    @Test
    void deletedEvent_mappingExists_createsDeletedConflict() {
        var mapping = seedMapping("gcal-deleted", "Deleted event", ORIGINAL_TIMING);

        int count = detector.detectConflicts(List.of(), List.of("gcal-deleted"));
        assertThat(count).isEqualTo(1);

        var conflicts = conflictStore.findUnresolved();
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.getFirst().conflictType()).isEqualTo(CalendarConflictType.DELETED);

        var captor = ArgumentCaptor.forClass(CalendarConflictDetected.class);
        verify(detectedEvent).fire(captor.capture());
        assertThat(captor.getValue().conflictType()).isEqualTo(CalendarConflictType.DELETED);
    }

    @Test
    void deletedEvent_noMapping_skips() {
        int count = detector.detectConflicts(List.of(), List.of("gcal-no-mapping"));
        assertThat(count).isZero();
    }

    private LifeCalendarMapping seedMapping(String eventId, String summary, EventTiming timing) {
        var mapping = new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", eventId,
                LifeDomain.HEALTH, summary, timing, Instant.now(), Instant.now());
        return mappingStore.save(mapping);
    }

    private static CalendarEvent calendarEvent(String id, String summary, EventTiming timing) {
        return new CalendarEvent(id, "primary", summary, null, null, timing, List.of(), null);
    }
}
