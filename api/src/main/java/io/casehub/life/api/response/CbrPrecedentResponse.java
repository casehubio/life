package io.casehub.life.api.response;

public record CbrPrecedentResponse(
        String caseId,
        double similarity,
        String outcome,
        String resolutionTime
) {}
