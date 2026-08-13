package io.casehub.life.app.commitment;

import io.casehub.life.api.request.CommitmentRequest;
import io.casehub.life.app.entity.ExternalActor;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.runtime.model.WorkItemEntity;

public record ContractorContext(
        CommitmentRequest request,
        WorkItemEntity workItem,
        LifeTaskContext taskContext,
        ExternalActor externalActor
) implements CommitmentContext {}
