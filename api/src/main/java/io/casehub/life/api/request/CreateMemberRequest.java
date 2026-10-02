package io.casehub.life.api.request;

import io.casehub.life.api.MemberRelationship;
import java.util.UUID;

public record CreateMemberRequest(
    String name, String email, String role,
    MemberRelationship relationship, UUID relatedTo,
    String notificationChannel, String notificationValue
) {}
