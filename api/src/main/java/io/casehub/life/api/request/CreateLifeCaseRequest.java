package io.casehub.life.api.request;

import io.casehub.life.api.LifeCaseType;
import java.util.Map;

public record CreateLifeCaseRequest(
        LifeCaseType caseType,
        Map<String, Object> context
) {
    public CreateLifeCaseRequest {
        if (caseType == null) throw new IllegalArgumentException("caseType is required");
        if (context == null) context = Map.of();
    }
}
