package io.casehub.life.api.response;

import java.util.List;
import java.util.Map;

public record RoutingDecisionResponse(
        String capabilityTag,
        String strategyId,
        CandidateScoreResponse selected,
        List<CandidateScoreResponse> alternatives,
        RoutingPolicySummary policy
) {

    public record CandidateScoreResponse(
            String workerId,
            Double trustScore,
            double workloadScore,
            String phase,
            int observations,
            double finalScore,
            String exclusionReason,
            String rationale,
            Map<String, Double> additionalScores
    ) {}

    public record RoutingPolicySummary(
            double threshold,
            double borderlineMargin,
            double blendFactor,
            int minimumObservations,
            Map<String, Double> qualityFloors,
            double cbrWeight,
            boolean bootstrapEscalationRequired
    ) {}
}
