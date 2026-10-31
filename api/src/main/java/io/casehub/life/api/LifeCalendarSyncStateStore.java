package io.casehub.life.api;

import java.util.Optional;

public interface LifeCalendarSyncStateStore {

    Optional<LifeCalendarSyncState> findByCalendar(String platformId, String calendarId);

    LifeCalendarSyncState save(LifeCalendarSyncState state);
}
