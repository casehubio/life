package io.casehub.life.app;

import io.casehub.ledger.api.model.LedgerEntryType;
import io.casehub.ledger.api.spi.LedgerEntryRepository;
import io.casehub.ledger.model.WorkerDecisionEntry;
import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.platform.api.identity.ActorType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class CaseRoutingQueryTest {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    @Inject LedgerEntryRepository ledgerEntryRepository;
    @Inject io.casehub.platform.testing.FixedCurrentPrincipal fixedPrincipal;

    private UUID caseTrackerId;
    private UUID engineCaseId;
    private UUID emptyCaseTrackerId;

    @BeforeEach
    @Transactional
    void seed() {
        fixedPrincipal.setGroups(Set.of("household-admin"));
        LifeCaseTracker.deleteAll();
        LifeTestFixtures.seedStandardTemplates();

        engineCaseId = UUID.randomUUID();
        LifeCaseTracker tracker = new LifeCaseTracker();
        tracker.caseType = "contractor-coordination";
        tracker.domain = LifeDomain.CONTRACTOR_COORDINATION;
        tracker.status = LifeCaseStatus.ACTIVE;
        tracker.engineCaseId = engineCaseId;
        tracker.createdAt = Instant.now();
        tracker.persist();
        caseTrackerId = tracker.id;

        seedWorkerDecision(engineCaseId, "quote-worker", "contractor-coordination", 0.85, 0.7, 1);
        seedWorkerDecision(engineCaseId, "monitor-worker", "household-management", 0.92, 0.7, 2);

        LifeCaseTracker emptyCase = new LifeCaseTracker();
        emptyCase.caseType = "travel-plan";
        emptyCase.domain = LifeDomain.TRAVEL;
        emptyCase.status = LifeCaseStatus.ACTIVE;
        emptyCase.engineCaseId = UUID.randomUUID();
        emptyCase.createdAt = Instant.now();
        emptyCase.persist();
        emptyCaseTrackerId = emptyCase.id;
    }

    @AfterEach
    void resetPrincipal() {
        fixedPrincipal.reset();
    }

    @Test
    void routing_returnsScopedDecisions() {
        given()
        .when()
            .get("/life-cases/{id}/routing", caseTrackerId)
        .then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("[0].capabilityTag", equalTo("contractor-coordination"))
            .body("[0].strategyId", equalTo("trust-weighted"))
            .body("[0].selected.workerId", equalTo("quote-worker"))
            .body("[0].selected.trustScore", equalTo(0.85f))
            .body("[0].policy", notNullValue())
            .body("[0].policy.threshold", notNullValue())
            .body("[1].capabilityTag", equalTo("household-management"))
            .body("[1].selected.workerId", equalTo("monitor-worker"));
    }

    @Test
    void routing_policyDerivedFromCapability() {
        given()
        .when()
            .get("/life-cases/{id}/routing", caseTrackerId)
        .then()
            .statusCode(200)
            .body("[0].policy.threshold", notNullValue())
            .body("[0].policy.minimumObservations", notNullValue())
            .body("[0].alternatives", empty());
    }

    @Test
    void routing_unknownCase_returns404() {
        given()
        .when()
            .get("/life-cases/{id}/routing", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void routing_noDecisions_returnsEmptyList() {
        given()
        .when()
            .get("/life-cases/{id}/routing", emptyCaseTrackerId)
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Transactional
    void seedWorkerDecision(UUID caseId, String workerId, String capabilityTag,
                            Double trustScore, Double threshold, int seq) {
        WorkerDecisionEntry entry = new WorkerDecisionEntry();
        entry.caseId = caseId;
        entry.workerId = workerId;
        entry.capabilityTag = capabilityTag;
        entry.trustScoreAtRouting = trustScore;
        entry.thresholdApplied = threshold;
        entry.routingRationale = "Test routing: score " + trustScore + " vs threshold " + threshold;
        entry.subjectId = caseId;
        entry.actorId = "life-system";
        entry.actorType = ActorType.SYSTEM;
        entry.entryType = LedgerEntryType.EVENT;
        entry.sequenceNumber = seq;
        entry.tenancyId = TENANCY_ID;
        entry.occurredAt = Instant.now();
        ledgerEntryRepository.save(entry, TENANCY_ID);
    }
}
