package io.casehub.life.api;

import io.casehub.connectors.calendar.spi.EventTiming;

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
        EventTiming pushedTiming,
        Instant createdAt,
        Instant updatedAt) {
}
