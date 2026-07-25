package io.casehub.life.app.engine.agent;

import java.util.List;

public record BookingResult(
        String appointmentId, String provider,
        boolean confirmed, Boolean declined, String reason,
        String calendarEventId, List<String> toolsUsed) {}
