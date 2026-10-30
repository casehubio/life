package io.casehub.life.app.service;

import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLifeCalendarMappingStore implements LifeCalendarMappingStore {

    private final Map<UUID, LifeCalendarMapping> store = new ConcurrentHashMap<>();

    @Override
    public LifeCalendarMapping save(LifeCalendarMapping mapping) {
        store.put(mapping.id(), mapping);
        return mapping;
    }

    @Override
    public Optional<LifeCalendarMapping> findByEntity(CalendarEntityType entityType, String entityId) {
        return store.values().stream()
                .filter(m -> m.entityType() == entityType && m.entityId().equals(entityId))
                .findFirst();
    }

    @Override
    public List<LifeCalendarMapping> findByCalendarEventId(String calendarEventId) {
        return store.values().stream()
                .filter(m -> m.calendarEventId().equals(calendarEventId))
                .toList();
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
    }
}
