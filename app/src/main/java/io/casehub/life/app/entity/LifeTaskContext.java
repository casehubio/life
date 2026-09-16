package io.casehub.life.app.entity;

import io.casehub.life.api.LifeDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "life_task_context")
@NamedQuery(name = "LifeTaskContext.findByExternalActorId",
        query = "SELECT c FROM LifeTaskContext c WHERE c.externalActorId = :externalActorId")
@NamedQuery(name = "LifeTaskContext.countByExternalActorId",
        query = "SELECT COUNT(c) FROM LifeTaskContext c WHERE c.externalActorId = :externalActorId")
public class LifeTaskContext {

    @Id
    @Column(name = "work_item_id")
    public UUID workItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public LifeDomain domain;

    @Column(name = "external_actor_id")
    public UUID externalActorId;

    @Column(length = 100)
    public String recurrence;

    @Column(length = 10)
    public String jurisdiction;
}
