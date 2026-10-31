package io.casehub.life.api;

import java.time.Instant;
import java.util.UUID;

public record LifeCalendarSyncState(
        UUID id,
        String platformId,
        String calendarId,
        String syncToken,
        Instant lastSyncAt) {
}
