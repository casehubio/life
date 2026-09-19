package io.casehub.life.app.entity;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.MemberRelationship;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "household_member")
public class HouseholdMember extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "household_id", nullable = false)
    public UUID householdId;

    @Column(name = "keycloak_user_id", nullable = false)
    public String keycloakUserId;

    @Column(nullable = false)
    public String name;

    public String email;

    @Column(nullable = false, length = 32)
    public String role;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    public MemberRelationship relationship;

    @Column(name = "related_to")
    public UUID relatedTo;

    @Column(name = "notification_channel", length = 32)
    public String notificationChannel;

    @Column(name = "notification_value")
    public String notificationValue;

    @Column(name = "joined_at", nullable = false, updatable = false)
    public Instant joinedAt;

    @Column(name = "deactivated_at")
    public Instant deactivatedAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (joinedAt == null) joinedAt = Instant.now();
        if (!Set.of(HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR).contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public static List<HouseholdMember> findByHouseholdId(UUID householdId) {
        return list("householdId = ?1 AND deactivatedAt IS NULL", householdId);
    }

    public static Optional<HouseholdMember> findByKeycloakUserId(String keycloakUserId) {
        return find("keycloakUserId", keycloakUserId).firstResultOptional();
    }

    public boolean isActive() {
        return deactivatedAt == null;
    }
}
