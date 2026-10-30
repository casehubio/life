package io.casehub.life.api;

import java.time.Instant;
import java.util.UUID;

public record LifeCalendarMapping(
        UUID id,
        CalendarEntityType entityType,
        String entityId,
        String calendarPlatformId,
        String calendarId,
        String calendarEventId,
        LifeDomain domain,
        String summary,
        Instant createdAt,
        Instant updatedAt) {
}
