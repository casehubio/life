package io.casehub.life.app.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "household")
public class Household extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, length = 64)
    public String timezone;

    @Column(length = 10)
    public String jurisdiction;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (timezone == null) timezone = "UTC";
    }

    public static Optional<Household> findByTenancyId(UUID tenancyId) {
        return findByIdOptional(tenancyId);
    }
}
