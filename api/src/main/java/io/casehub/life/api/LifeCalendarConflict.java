package io.casehub.life.api;

import io.casehub.connectors.calendar.spi.EventTiming;

import java.time.Instant;
import java.util.UUID;

public record LifeCalendarConflict(
        UUID id,
        UUID mappingId,
        CalendarConflictType conflictType,
        Instant detectedAt,
        String externalSummary,
        EventTiming externalTiming,
        CalendarConflictStatus status,
        Instant resolvedAt,
        String resolvedBy,
        LifeDomain domain) {
}
