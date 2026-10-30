package io.casehub.life.api.response;

import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeDomain;

import java.util.List;

public record LifeCalendarEventResponse(
        String eventId,
        String calendarId,
        String summary,
        String description,
        String location,
        EventTiming timing,
        List<String> attendees,
        LifeDomain domain,
        CalendarEntityType entityType,
        String entityId,
        boolean casehubManaged) {
}
