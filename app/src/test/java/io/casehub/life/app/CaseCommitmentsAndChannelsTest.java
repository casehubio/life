package io.casehub.life.app;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.commitment.CommitmentMode;
import io.casehub.life.api.commitment.CommitmentStatus;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.life.app.entity.LifeCommitmentRecord;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.platform.testing.FixedCurrentPrincipal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class CaseCommitmentsAndChannelsTest {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    @Inject FixedCurrentPrincipal fixedPrincipal;

    private UUID caseTrackerId;
    private UUID engineCaseId;
    private UUID emptyCaseTrackerId;

    @BeforeEach
    @Transactional
    void seed() {
        fixedPrincipal.setGroups(java.util.Set.of(HouseholdGroups.ADMIN));
        LifeCommitmentRecord.deleteAll();
        LifeTaskContext.deleteAll();
        WorkItemEntity.deleteAll();
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

        UUID wiId = seedWorkItem("Request Quote",
                "case:" + engineCaseId + "/pi:request-quote",
                WorkItemStatus.PENDING);

        LifeCommitmentRecord rec = new LifeCommitmentRecord();
        rec.id = UUID.randomUUID();
        rec.correlationId = "test-commit-" + UUID.randomUUID();
        rec.mode = CommitmentMode.CONTRACTOR;
        rec.status = CommitmentStatus.PENDING_RESPONSE;
        rec.workItemId = wiId;
        rec.domain = LifeDomain.CONTRACTOR_COORDINATION;
        rec.channelId = "life/actor/ext-001";
        rec.amountThreshold = new BigDecimal("450.00");
        rec.createdAt = Instant.now();
        rec.updatedAt = Instant.now();
        rec.persist();

        LifeCaseTracker emptyCase = new LifeCaseTracker();
        emptyCase.caseType = "travel-plan";
        emptyCase.domain = LifeDomain.TRAVEL;
        emptyCase.status = LifeCaseStatus.ACTIVE;
        emptyCase.engineCaseId = UUID.randomUUID();
        emptyCase.createdAt = Instant.now();
        emptyCase.persist();
        emptyCaseTrackerId = emptyCase.id;
    }

    @Test
    void commitments_returnsScopedRecords() {
        given()
        .when()
            .get("/life-cases/{id}/commitments", caseTrackerId)
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].mode", equalTo("CONTRACTOR"))
            .body("[0].status", equalTo("PENDING_RESPONSE"));
    }

    @Test
    void commitments_unknownCase_returns404() {
        given()
        .when()
            .get("/life-cases/{id}/commitments", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void commitments_noneLinked_returnsEmpty() {
        given()
        .when()
            .get("/life-cases/{id}/commitments", emptyCaseTrackerId)
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    private UUID seedWorkItem(final String title, final String callerRef,
                               final WorkItemStatus status) {
        WorkItemEntity wi = new WorkItemEntity();
        wi.id = UUID.randomUUID();
        wi.title = title;
        wi.callerRef = callerRef;
        wi.scope = "casehubio/life/contractor-coordination";
        wi.status = status;
        wi.candidateGroups = "household-admin";
        wi.createdAt = Instant.now();
        wi.tenancyId = TENANCY_ID;
        wi.persist();
        return wi.id;
    }
}
