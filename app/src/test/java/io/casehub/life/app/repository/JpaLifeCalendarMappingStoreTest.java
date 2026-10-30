package io.casehub.life.app.repository;

import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.api.LifeDomain;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaLifeCalendarMappingStoreTest {

    @Inject
    LifeCalendarMappingStore store;

    @Test
    @Transactional
    void saveAndFindByEntity() {
        var mapping = new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.WORK_ITEM, UUID.randomUUID().toString(),
                "google", "primary", "gcal-" + UUID.randomUUID(),
                LifeDomain.HEALTH, "GP check-up", Instant.now(), Instant.now());

        store.save(mapping);

        var found = store.findByEntity(CalendarEntityType.WORK_ITEM, mapping.entityId());
        assertThat(found).isPresent();
        assertThat(found.get().calendarEventId()).isEqualTo(mapping.calendarEventId());
        assertThat(found.get().domain()).isEqualTo(LifeDomain.HEALTH);
    }

    @Test
    @Transactional
    void findByCalendarEventId() {
        var eventId = "gcal-" + UUID.randomUUID();
        var mapping = new LifeCalendarMapping(
                UUID.randomUUID(), CalendarEntityType.CASE_MILESTONE, "case-1:milestone-a-" + UUID.randomUUID(),
                "google", "primary", eventId,
                LifeDomain.TRAVEL, "Trip deadline", Instant.now(), Instant.now());

        store.save(mapping);

        var found = store.findByCalendarEventId(eventId);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().entityType()).isEqualTo(CalendarEntityType.CASE_MILESTONE);
    }

    @Test
    @Transactional
    void deleteRemovesMapping() {
        var id = UUID.randomUUID();
        var entityId = UUID.randomUUID().toString();
        var mapping = new LifeCalendarMapping(
                id, CalendarEntityType.WORK_ITEM, entityId,
                "google", "primary", "gcal-" + UUID.randomUUID(),
                LifeDomain.FINANCE, "Budget review", Instant.now(), Instant.now());

        store.save(mapping);
        assertThat(store.findByEntity(CalendarEntityType.WORK_ITEM, entityId)).isPresent();

        store.delete(id);
        assertThat(store.findByEntity(CalendarEntityType.WORK_ITEM, entityId)).isEmpty();
    }
}
