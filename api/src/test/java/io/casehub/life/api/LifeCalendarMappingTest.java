package io.casehub.life.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LifeCalendarMappingTest {

    @Test
    void recordFieldsAreAccessible() {
        var now = Instant.now();
        var id = UUID.randomUUID();
        var mapping = new LifeCalendarMapping(
                id, CalendarEntityType.WORK_ITEM, "wi-123",
                "google", "primary", "gcal-event-456",
                LifeDomain.HEALTH, "GP check-up", now, now);

        assertThat(mapping.entityType()).isEqualTo(CalendarEntityType.WORK_ITEM);
        assertThat(mapping.entityId()).isEqualTo("wi-123");
        assertThat(mapping.calendarPlatformId()).isEqualTo("google");
        assertThat(mapping.calendarEventId()).isEqualTo("gcal-event-456");
        assertThat(mapping.domain()).isEqualTo(LifeDomain.HEALTH);
        assertThat(mapping.summary()).isEqualTo("GP check-up");
    }
}
