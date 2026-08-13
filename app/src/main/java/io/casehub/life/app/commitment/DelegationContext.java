package io.casehub.life.app.commitment;

import io.casehub.life.api.request.CommitmentRequest;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.runtime.model.WorkItemEntity;

public record DelegationContext(
        CommitmentRequest request,
        WorkItemEntity workItem,
        LifeTaskContext taskContext
) implements CommitmentContext {}
