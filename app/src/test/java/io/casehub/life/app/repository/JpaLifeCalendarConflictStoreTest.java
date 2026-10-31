package io.casehub.life.app.repository;

import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.api.LifeDomain;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaLifeCalendarConflictStoreTest {

    @Inject
    JpaLifeCalendarConflictStore conflictStore;

    @Inject
    LifeCalendarMappingStore mappingStore;

    @Test
    @Transactional
    void save_and_findById() {
        var mapping = seedMapping();
        var timing = new EventTiming.AllDay(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6));
        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                "Changed summary", timing, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH);

        conflictStore.save(conflict);

        var found = conflictStore.findById(conflict.id());
        assertThat(found).isPresent();
        assertThat(found.get().conflictType()).isEqualTo(CalendarConflictType.MODIFIED);
        assertThat(found.get().externalSummary()).isEqualTo("Changed summary");
        assertThat(found.get().externalTiming()).isInstanceOf(EventTiming.AllDay.class);
    }

    @Test
    @Transactional
    void findUnresolved_returns_only_detected() {
        var mapping = seedMapping();
        var detected = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HOUSEHOLD);
        var accepted = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                null, null, CalendarConflictStatus.ACCEPTED,
                Instant.now(), "user-1", LifeDomain.HOUSEHOLD);

        conflictStore.save(detected);
        conflictStore.save(accepted);

        var unresolved = conflictStore.findUnresolved();
        assertThat(unresolved).hasSize(1);
        assertThat(unresolved.getFirst().id()).isEqualTo(detected.id());
    }

    @Test
    @Transactional
    void findUnresolvedByMapping() {
        var mapping = seedMapping();
        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.DELETED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.FINANCE);

        conflictStore.save(conflict);

        assertThat(conflictStore.findUnresolvedByMapping(mapping.id())).isPresent();
        assertThat(conflictStore.findUnresolvedByMapping(UUID.randomUUID())).isEmpty();
    }

    @Test
    @Transactional
    void countUnresolved() {
        var mapping = seedMapping();
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.DELETED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HEALTH));

        assertThat(conflictStore.countUnresolved()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    void deleteByMapping_removes_all_conflicts_for_mapping() {
        var mapping = seedMapping();
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, LifeDomain.HOUSEHOLD));
        conflictStore.save(new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.DELETED, Instant.now(),
                null, null, CalendarConflictStatus.ACCEPTED,
                Instant.now(), "admin", LifeDomain.HOUSEHOLD));

        conflictStore.deleteByMapping(mapping.id());

        assertThat(conflictStore.findByMapping(mapping.id())).isEmpty();
    }

    private LifeCalendarMapping seedMapping() {
        return mappingStore.save(new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", "gcal-" + UUID.randomUUID(),
                LifeDomain.HEALTH, "Test event", null, Instant.now(), Instant.now()));
    }
}
