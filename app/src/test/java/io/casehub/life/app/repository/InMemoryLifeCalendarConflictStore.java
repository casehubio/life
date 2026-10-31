package io.casehub.life.app.repository;

import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarConflictStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLifeCalendarConflictStore implements LifeCalendarConflictStore {

    private final Map<UUID, LifeCalendarConflict> store = new ConcurrentHashMap<>();

    @Override
    public LifeCalendarConflict save(LifeCalendarConflict conflict) {
        store.put(conflict.id(), conflict);
        return conflict;
    }

    @Override
    public Optional<LifeCalendarConflict> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<LifeCalendarConflict> findByMapping(UUID mappingId) {
        return store.values().stream()
                .filter(c -> c.mappingId().equals(mappingId))
                .toList();
    }

    @Override
    public List<LifeCalendarConflict> findUnresolved() {
        return store.values().stream()
                .filter(c -> c.status() == CalendarConflictStatus.DETECTED)
                .toList();
    }

    @Override
    public long countUnresolved() {
        return store.values().stream()
                .filter(c -> c.status() == CalendarConflictStatus.DETECTED)
                .count();
    }

    @Override
    public Optional<LifeCalendarConflict> findUnresolvedByMapping(UUID mappingId) {
        return store.values().stream()
                .filter(c -> c.mappingId().equals(mappingId) && c.status() == CalendarConflictStatus.DETECTED)
                .findFirst();
    }

    @Override
    public void deleteByMapping(UUID mappingId) {
        store.entrySet().removeIf(e -> e.getValue().mappingId().equals(mappingId));
    }
}
