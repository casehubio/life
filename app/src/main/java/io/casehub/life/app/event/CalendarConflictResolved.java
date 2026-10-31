package io.casehub.life.app.event;

import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.LifeDomain;

import java.util.UUID;

public record CalendarConflictResolved(
        UUID conflictId,
        CalendarConflictStatus resolution,
        String resolvedBy,
        LifeDomain domain,
        String entityId) {
}
