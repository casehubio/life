package io.casehub.life.app;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class CaseScopedTasksTest {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    @Inject
    io.casehub.platform.testing.FixedCurrentPrincipal fixedPrincipal;

    private UUID caseTrackerId;
    private UUID engineCaseId;
    private UUID emptyCaseTrackerId;

    @BeforeEach
    @Transactional
    void seed() {
        fixedPrincipal.setGroups(java.util.Set.of("household-admin"));
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

        seedWorkItem("Request Quote", "case:" + engineCaseId + "/pi:request-quote",
                "casehubio/life/contractor-coordination", WorkItemStatus.PENDING);
        seedWorkItem("Monitor Job", "case:" + engineCaseId + "/pi:monitor-job",
                "casehubio/life/contractor-coordination", WorkItemStatus.ASSIGNED);
        seedWorkItem("Unrelated Task", "life:task/grocery-run",
                "casehubio/life/household", WorkItemStatus.PENDING);

        LifeCaseTracker emptyCase = new LifeCaseTracker();
        emptyCase.caseType = "travel-plan";
        emptyCase.domain = LifeDomain.TRAVEL;
        emptyCase.status = LifeCaseStatus.ACTIVE;
        emptyCase.engineCaseId = UUID.randomUUID();
        emptyCase.createdAt = Instant.now();
        emptyCase.persist();
        emptyCaseTrackerId = emptyCase.id;
    }

    @org.junit.jupiter.api.AfterEach
    void resetPrincipal() {
        fixedPrincipal.reset();
    }


    @Test
    void caseTasks_returnsScopedWorkItems() {
        given()
        .when()
            .get("/life-cases/{id}/tasks", caseTrackerId)
        .then()
            .statusCode(200)
            .body("items", hasSize(2))
            .body("items[0].title", equalTo("Request Quote"));
    }

    @Test
    void caseTasks_excludesUnrelatedWorkItems() {
        given()
        .when()
            .get("/life-cases/{id}/tasks", caseTrackerId)
        .then()
            .statusCode(200)
            .body("items", hasSize(2));
    }

    @Test
    void caseTasks_unknownCase_returns404() {
        given()
        .when()
            .get("/life-cases/{id}/tasks", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void caseTasks_noWorkItems_returnsEmptyList() {
        given()
                .when()
                .get("/life-cases/{id}/tasks", emptyCaseTrackerId)
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    @Test
    @TestSecurity(user = "junior-user", roles = {"household-junior"})
    void caseTasks_juniorSeesOnlyAssignedTasks() {
        fixedPrincipal.setGroups(java.util.Set.of("household-junior"));

        given()
                .when()
                .get("/life-cases/{id}/tasks", caseTrackerId)
                .then()
                .statusCode(404);}


    private void seedWorkItem(final String title, final String callerRef,
                               final String scope, final WorkItemStatus status) {
        WorkItemEntity wi = new WorkItemEntity();
        wi.id = UUID.randomUUID();
        wi.title = title;
        wi.callerRef = callerRef;
        wi.scope = scope;
        wi.status = status;
        wi.candidateGroups = "household-admin,household-member";
        wi.createdAt = Instant.now();
        wi.tenancyId = TENANCY_ID;
        wi.persist();
    }
}
