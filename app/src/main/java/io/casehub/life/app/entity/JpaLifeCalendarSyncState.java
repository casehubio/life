package io.casehub.life.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "life_calendar_sync_state")
public class JpaLifeCalendarSyncState {

    @Id
    public UUID id;

    @Column(name = "platform_id", nullable = false, length = 50)
    public String platformId;

    @Column(name = "calendar_id", nullable = false)
    public String calendarId;

    @Column(name = "sync_token", length = 1024)
    public String syncToken;

    @Column(name = "last_sync_at")
    public Instant lastSyncAt;
}
