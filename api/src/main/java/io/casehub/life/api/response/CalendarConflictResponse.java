package io.casehub.life.api.response;

import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.LifeDomain;

import java.time.Instant;
import java.util.UUID;

public record CalendarConflictResponse(
        UUID id,
        UUID mappingId,
        CalendarConflictType conflictType,
        Instant detectedAt,
        String externalSummary,
        EventTiming externalTiming,
        CalendarConflictStatus status,
        Instant resolvedAt,
        String resolvedBy,
        LifeDomain domain,
        String entityId,
        String casehubSummary,
        EventTiming casehubTiming) {
}
