package io.casehub.life.app.entity;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "life_case_tracker")
@NamedQuery(name = "LifeCaseTracker.findByEngineCaseId",
        query = "SELECT t FROM LifeCaseTracker t WHERE t.engineCaseId = :engineCaseId")
@NamedQuery(name = "LifeCaseTracker.findByCaseType",
        query = "SELECT t FROM LifeCaseTracker t WHERE t.caseType = :caseType")
@NamedQuery(name = "LifeCaseTracker.findAll",
        query = "SELECT t FROM LifeCaseTracker t")
public class LifeCaseTracker {

    @Id
    public UUID id;

    @Column(name = "case_type", nullable = false, length = 64)
    public String     caseType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public LifeDomain domain;


    @Column(name = "engine_case_id", unique = true)
    public UUID engineCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public LifeCaseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "completed_at")
    public Instant completedAt;
    @Column(name = "cbr_precedents_json", columnDefinition = "TEXT")
    public String  cbrPrecedentsJson;


    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = LifeCaseStatus.ACTIVE;
    }

}
