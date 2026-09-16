package io.casehub.life.app.entity;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.commitment.CommitmentMode;
import io.casehub.life.api.commitment.CommitmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "life_commitment_record")
@NamedQuery(name = "LifeCommitmentRecord.findByCorrelationId",
        query = "SELECT r FROM LifeCommitmentRecord r WHERE r.correlationId = :correlationId")
@NamedQuery(name = "LifeCommitmentRecord.findByWorkItemId",
        query = "SELECT r FROM LifeCommitmentRecord r WHERE r.workItemId = :workItemId AND r.status <> :excludedStatus")
@NamedQuery(name = "LifeCommitmentRecord.findExpiredPendingByChannel",
        query = "SELECT r FROM LifeCommitmentRecord r WHERE r.channelId = :channelId AND r.status = :status AND r.deadline <= :now")
@NamedQuery(name = "LifeCommitmentRecord.findByWorkItemIds",
        query = "SELECT r FROM LifeCommitmentRecord r WHERE r.workItemId IN :workItemIds")
@NamedQuery(name = "LifeCommitmentRecord.countByModeStatusKey",
        query = "SELECT COUNT(r) FROM LifeCommitmentRecord r WHERE r.mode = :mode AND r.status = :status AND r.oversightKey = :oversightKey")
public class LifeCommitmentRecord {

    @Id
    public UUID id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 255)
    public String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public CommitmentMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public CommitmentStatus status;

    @Column(name = "work_item_id")
    public UUID workItemId;              // null for OVERSIGHT until RESPONSE fulfills gate

    @Column(name = "external_actor_id")
    public UUID externalActorId;        // CONTRACTOR only

    @Column(name = "delegate_to", length = 255)
    public String delegateTo;           // DELEGATION: principal id; null for other modes

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    public LifeDomain domain;

    @Column(name = "oversight_key", length = 255)
    public String oversightKey;         // OVERSIGHT only: dedup key (title:templateRef)

    @Column(name = "channel_id", nullable = false, length = 255)
    public String channelId;

    public Instant deadline;

    @Column(name = "pending_task_json", columnDefinition = "TEXT")
    public String pendingTaskJson;      // OVERSIGHT only — serialized CreateLifeTaskRequest

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "approved_by", length = 255)
    public String approvedBy;

    @Column(name = "amount_threshold", precision = 15, scale = 2)
    public java.math.BigDecimal amountThreshold;

    @Column(name = "purchase_category", length = 100)
    public String purchaseCategory;


}
