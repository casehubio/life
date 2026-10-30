package io.casehub.life.app.entity;

import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "life_calendar_mapping")
public class JpaLifeCalendarMapping {

    @Id
    public UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    public CalendarEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    public String entityId;

    @Column(name = "calendar_platform_id", nullable = false)
    public String calendarPlatformId;

    @Column(name = "calendar_id", nullable = false)
    public String calendarId;

    @Column(name = "calendar_event_id", nullable = false)
    public String calendarEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LifeDomain domain;

    @Column(length = 500)
    public String summary;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
