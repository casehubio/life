package io.casehub.life.api.response;

import io.casehub.life.api.MemberRelationship;
import java.time.Instant;
import java.util.UUID;

public record HouseholdMemberResponse(
    UUID id, String name, String email, String role,
    MemberRelationship relationship, UUID relatedTo,
    String notificationChannel, String notificationValue,
    Instant joinedAt, boolean active
) {}
