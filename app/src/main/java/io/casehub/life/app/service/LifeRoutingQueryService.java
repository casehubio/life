package io.casehub.life.app.service;

import io.casehub.api.spi.routing.TrustRoutingPolicy;
import io.casehub.ledger.model.WorkerDecisionEntry;
import io.casehub.ledger.repository.CaseLedgerEntryRepository;
import io.casehub.life.api.response.RoutingDecisionResponse;
import io.casehub.life.api.response.RoutingDecisionResponse.CandidateScoreResponse;
import io.casehub.life.api.response.RoutingDecisionResponse.RoutingPolicySummary;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.life.app.routing.LifeTrustRoutingPolicyProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LifeRoutingQueryService {

    @Inject CaseLedgerEntryRepository caseLedgerEntryRepository;
    @Inject LifeTrustRoutingPolicyProvider routingPolicyProvider;

    @Transactional
    public Optional<List<RoutingDecisionResponse>> findRoutingByCase(UUID caseTrackerId) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(caseTrackerId);
        if (tracker == null) return Optional.empty();
        if (tracker.engineCaseId == null) return Optional.of(List.of());

        List<WorkerDecisionEntry> entries =
                caseLedgerEntryRepository.findWorkerDecisionsByCaseId(tracker.engineCaseId);

        List<RoutingDecisionResponse> responses = entries.stream()
                .map(this::toResponse)
                .toList();

        return Optional.of(responses);
    }

    private RoutingDecisionResponse toResponse(WorkerDecisionEntry entry) {
        TrustRoutingPolicy policy = routingPolicyProvider.forCapability(entry.capabilityTag);

        String phase = derivePhase(entry.trustScoreAtRouting, policy);
        double finalScore = entry.trustScoreAtRouting != null ? entry.trustScoreAtRouting : 0.0;

        CandidateScoreResponse selected = new CandidateScoreResponse(
                entry.workerId,
                entry.trustScoreAtRouting,
                0.0,
                phase,
                0,
                finalScore,
                null,
                entry.routingRationale,
                Map.of()
        );

        RoutingPolicySummary policySummary = new RoutingPolicySummary(
                policy.threshold(),
                policy.borderlineMargin(),
                policy.blendFactor(),
                policy.minimumObservations(),
                policy.qualityFloors(),
                policy.cbrWeight(),
                policy.bootstrapEscalationRequired()
        );

        return new RoutingDecisionResponse(
                entry.capabilityTag,
                "trust-weighted",
                selected,
                List.of(),
                policySummary
        );
    }

    private String derivePhase(Double trustScore, TrustRoutingPolicy policy) {
        if (trustScore == null) return "BOOTSTRAP";
        if (policy.isBorderline(trustScore)) return "BORDERLINE";
        if (policy.passesThresholdCheck(trustScore)) return "QUALIFIED";
        return "BOOTSTRAP";
    }
}
