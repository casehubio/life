package io.casehub.life.api.response;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.commitment.CommitmentMode;
import io.casehub.life.api.commitment.CommitmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LifeTaskResponse(
        UUID workItemId,
        String templateRef,
        LifeDomain domain,
        String status,
        UUID externalActorId,          // nullable
        Instant createdAt,
        CommitmentMode commitmentMode,  // null if no commitment on this task
        CommitmentStatus commitmentStatus,
        String assigneeId,             // nullable — identity of the actor assigned to this task
        List<String> candidateGroups   // groups eligible to claim this task
) {}
