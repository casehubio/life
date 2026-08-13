package io.casehub.life.app;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.commitment.CommitmentMode;
import io.casehub.life.api.commitment.CommitmentStatus;
import io.casehub.life.app.entity.LifeCommitmentRecord;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class PendingActionsActionTypeTest {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    private UUID standardWiId;
    private UUID oversightWiId;
    private UUID delegationWiId;
    private UUID escalationWiId;

    @BeforeEach
    @Transactional
    void seed() {
        LifeCommitmentRecord.deleteAll();
        LifeTaskContext.deleteAll();
        WorkItemEntity.deleteAll();
        LifeTestFixtures.seedStandardTemplates();

        standardWiId = seedWorkItem("Grocery Run", WorkItemStatus.PENDING);
        oversightWiId = seedWorkItem("Approve £500 purchase", WorkItemStatus.PENDING);
        seedCommitmentRecord(oversightWiId, CommitmentMode.OVERSIGHT);
        delegationWiId = seedWorkItem("Delegated task", WorkItemStatus.PENDING);
        seedCommitmentRecord(delegationWiId, CommitmentMode.DELEGATION);
        escalationWiId = seedWorkItem("SLA Breach Escalation", WorkItemStatus.PENDING,
                "life:task/life-escalation");
    }

    @Test
    void standardWorkItem_returnsWorkItemType() {
        given()
            .queryParam("size", 100)
        .when()
            .get("/pending-actions")
        .then()
            .statusCode(200)
            .body("items.find { it.workItemId == '" + standardWiId + "' }.actionType",
                    equalTo("WORK_ITEM"));
    }

    @Test
    void oversightLinkedWorkItem_returnsOversightGateType() {
        given()
            .queryParam("size", 100)
        .when()
            .get("/pending-actions")
        .then()
            .statusCode(200)
            .body("items.find { it.workItemId == '" + oversightWiId + "' }.actionType",
                    equalTo("OVERSIGHT_GATE"));
    }

    @Test
    void delegationLinkedWorkItem_returnsDelegationType() {
        given()
            .queryParam("size", 100)
        .when()
            .get("/pending-actions")
        .then()
            .statusCode(200)
            .body("items.find { it.workItemId == '" + delegationWiId + "' }.actionType",
                    equalTo("DELEGATION"));
    }

    @Test
    void escalationWorkItem_returnsWatchdogAlertType() {
        given()
                .queryParam("size", 100)
                .when()
                .get("/pending-actions")
                .then()
                .statusCode(200)
                .body("items.find { it.workItemId == '" + escalationWiId + "' }.actionType",
                      equalTo("WATCHDOG_ALERT"));
    }


    private UUID seedWorkItem(final String title, final WorkItemStatus status) {
        return seedWorkItem(title, status, null);
    }

    private UUID seedWorkItem(final String title, final WorkItemStatus status, final String callerRef) {
        WorkItemEntity wi = new WorkItemEntity();
        wi.id = UUID.randomUUID();
        wi.title = title;
        wi.scope = "casehubio/life/household";
        wi.status = status;
        wi.callerRef = callerRef;
        wi.candidateGroups = "household-admin,household-member";
        wi.createdAt = Instant.now();
        wi.tenancyId = TENANCY_ID;
        wi.persist();
        return wi.id;
    }

    private void seedCommitmentRecord(final UUID workItemId, final CommitmentMode mode) {
        LifeCommitmentRecord rec = new LifeCommitmentRecord();
        rec.id = UUID.randomUUID();
        rec.correlationId = "test-" + UUID.randomUUID();
        rec.mode = mode;
        rec.status = CommitmentStatus.PENDING_RESPONSE;
        rec.workItemId = workItemId;
        rec.domain = LifeDomain.FINANCE;
        rec.channelId = "life/oversight";
        rec.createdAt = Instant.now();
        rec.updatedAt = Instant.now();
        rec.persist();
    }
}
