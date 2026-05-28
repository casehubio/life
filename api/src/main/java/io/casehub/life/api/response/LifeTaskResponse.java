package io.casehub.life.api.response;

import io.casehub.life.api.LifeDomain;

import java.time.Instant;
import java.util.UUID;

public record LifeTaskResponse(
        UUID workItemId,
        String templateRef,
        LifeDomain domain,
        String status,
        UUID externalActorId,  // nullable
        Instant createdAt
) {}
