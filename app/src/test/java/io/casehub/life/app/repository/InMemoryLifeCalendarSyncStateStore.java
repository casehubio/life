package io.casehub.life.app.repository;

import io.casehub.life.api.LifeCalendarSyncState;
import io.casehub.life.api.LifeCalendarSyncStateStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLifeCalendarSyncStateStore implements LifeCalendarSyncStateStore {

    private final Map<String, LifeCalendarSyncState> store = new ConcurrentHashMap<>();

    @Override
    public LifeCalendarSyncState save(LifeCalendarSyncState state) {
        store.put(state.platformId() + ":" + state.calendarId(), state);
        return state;
    }

    @Override
    public Optional<LifeCalendarSyncState> findByCalendar(String platformId, String calendarId) {
        return Optional.ofNullable(store.get(platformId + ":" + calendarId));
    }
}
