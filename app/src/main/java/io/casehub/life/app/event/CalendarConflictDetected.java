package io.casehub.life.app.event;

import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.LifeDomain;

import java.util.UUID;

public record CalendarConflictDetected(
        UUID conflictId,
        CalendarConflictType conflictType,
        LifeDomain domain,
        String entityId,
        UUID mappingId) {
}
