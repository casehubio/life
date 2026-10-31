package io.casehub.life.app.repository;

import io.casehub.life.api.LifeCalendarSyncState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class JpaLifeCalendarSyncStateStoreTest {

    @Inject
    JpaLifeCalendarSyncStateStore store;

    @Test
    @Transactional
    void save_and_findByCalendar() {
        var state = new LifeCalendarSyncState(
                UUID.randomUUID(), "google", "primary", "token-1", Instant.now());
        store.save(state);

        var found = store.findByCalendar("google", "primary");
        assertTrue(found.isPresent());
        assertEquals("token-1", found.get().syncToken());
    }

    @Test
    @Transactional
    void save_updates_existing() {
        var id = UUID.randomUUID();
        store.save(new LifeCalendarSyncState(id, "google", "cal-update", "token-1", Instant.now()));
        store.save(new LifeCalendarSyncState(id, "google", "cal-update", "token-2", Instant.now()));

        var found = store.findByCalendar("google", "cal-update");
        assertTrue(found.isPresent());
        assertEquals("token-2", found.get().syncToken());
    }

    @Test
    void findByCalendar_returns_empty_when_not_found() {
        var found = store.findByCalendar("google", "nonexistent");
        assertTrue(found.isEmpty());
    }
}
