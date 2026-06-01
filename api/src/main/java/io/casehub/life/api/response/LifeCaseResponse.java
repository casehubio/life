package io.casehub.life.api.response;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeCaseType;
import java.util.UUID;

public record LifeCaseResponse(
        UUID caseId,
        LifeCaseType caseType,
        LifeCaseStatus status
) {}
