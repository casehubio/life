package io.casehub.life.app.entity;

import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
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
@Table(name = "life_calendar_conflict")
public class JpaLifeCalendarConflict {

    @Id
    public UUID id;

    @Column(name = "mapping_id", nullable = false)
    public UUID mappingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    public CalendarConflictType conflictType;

    @Column(name = "detected_at", nullable = false)
    public Instant detectedAt;

    @Column(name = "external_summary", length = 500)
    public String externalSummary;

    @Column(name = "external_timing", columnDefinition = "TEXT")
    public String externalTiming;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CalendarConflictStatus status;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "resolved_by")
    public String resolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LifeDomain domain;
}
