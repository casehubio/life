package io.casehub.life.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LifeCalendarMappingStore {

    LifeCalendarMapping save(LifeCalendarMapping mapping);

    Optional<LifeCalendarMapping> findByEntity(CalendarEntityType entityType, String entityId);

    List<LifeCalendarMapping> findByCalendarEventId(String calendarEventId);

    void delete(UUID id);
}
