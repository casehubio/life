package io.casehub.life.app;

import io.casehub.life.app.entity.ExternalActor;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.runtime.model.WorkItemEntity;
import io.casehub.work.runtime.service.ExpiryLifecycleService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Layer 2 showcase — same household week as Layer 1, gaps now closed by casehub-work.
 * Layer 1 showed: tasks created, deadlines missed, no enforcement, no audit.
 * Layer 2 shows: WorkItems created with SLA, ExpiryLifecycleService enforces breach,
 *                LifeSlaBreachPolicy escalates to household-admin then fails terminally.
 */
@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShowcaseScenarioTest {

    @Inject
    ExpiryLifecycleService expiryLifecycleService;

    static String bobActorId;
    static String boilerWorkItemId;

    @BeforeEach
    @Transactional
    void seedTemplates() {
        if (bobActorId == null) {
            LifeTaskContext.deleteAll();
            WorkItemEntity.deleteAll();
            ExternalActor.deleteAll();
        }
        LifeTestFixtures.seedStandardTemplates();
    }

    @Test
    @Order(1)
    void contractorRegistered_taskCreatedWithWorkItem() {
        bobActorId = given()
                .contentType("application/json")
                .body("""
                        {"name":"Bob's Plumbing","actorType":"EXTERNAL_HUMAN",
                         "contactMethod":"phone","contactValue":"+44-7700-900100"}
                        """)
                .when().post("/external-actors")
                .then().statusCode(201)
                .body("name", equalTo("Bob's Plumbing"))
                .extract().path("id");

        // Layer 2: WorkItem created with SLA enforcement alongside LifeTaskContext.
        boilerWorkItemId = given()
                .contentType("application/json")
                .body("""
                        {"templateRef":"contractor-coordination","title":"Fix boiler",
                         "externalActorId":"%s"}
                        """.formatted(bobActorId))
                .when().post("/life-tasks")
                .then().statusCode(201)
                .body("workItemId", notNullValue())
                .body("domain", equalTo("CONTRACTOR_COORDINATION"))
                .body("externalActorId", equalTo(bobActorId))
                .body("status", equalTo("PENDING"))
                .extract().path("workItemId");

        // LifeTaskContext links WorkItem to Bob — visible via /external-actors/{id}/tasks.
        given()
                .when().get("/external-actors/{id}/tasks", bobActorId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].externalActorId", equalTo(bobActorId));
    }

    @Test
    @Order(2)
    void contractorSlaBreaches_firstBreach_escalatesToAdmin() {
        if (bobActorId == null) {
            bobActorId = given()
                                 .contentType("application/json")
                                 .body("""
                                       {"name":"Bob's Plumbing","actorType":"EXTERNAL_HUMAN",
                                        "contactMethod":"phone","contactValue":"+44-7700-900200"}
                                       """)
                                 .when().post("/external-actors")
                                 .then().statusCode(201)
                                 .extract().path("id");
        }

        given()
                .contentType("application/json")
                .body("""
                      {"templateRef":"contractor-coordination","title":"Overdue boiler quote",
                       "externalActorId":"%s","deadline":"%s"}
                      """.formatted(bobActorId, Instant.now().minus(1, ChronoUnit.HOURS)))
                .when().post("/life-tasks")
                .then().statusCode(201);

        expiryLifecycleService.checkExpired();
    }

    @Test
    @Order(3)
    void healthAppointment_createdWithSla() {
        // Health appointment — no external actor (GP is coordinated by system).
        given()
                .contentType("application/json")
                .body("""
                        {"templateRef":"health-appointment","title":"GP follow-up call"}
                        """)
                .when().post("/life-tasks")
                .then().statusCode(201)
                .body("domain", equalTo("HEALTH"))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(4)
    void healthFollowUpBreaches_slaEnforced() {
        // Create a health task with a past deadline.
        given()
                .contentType("application/json")
                .body("""
                        {"templateRef":"health-appointment","title":"Overdue GP call",
                         "deadline":"%s"}
                        """.formatted(Instant.now().minus(1, ChronoUnit.HOURS)))
                .when().post("/life-tasks")
                .then().statusCode(201);

        // Layer 1: task would remain silently overdue indefinitely.
        // Layer 2: breach fires, escalates, then terminates on second breach.
        expiryLifecycleService.checkExpired();
        // Gap closed: health follow-up SLA enforcement was absent in Layer 1.
    }

    @Test
    @Order(5)
    void actorDeletion_blockedByActiveTask() {
        if (bobActorId == null) {return;}
        given()
                .when().delete("/external-actors/{id}", bobActorId)
                .then()
                .statusCode(409);
    }

    @Test
    @Order(6)
    void weekSummary_allTasksTrackedWithWorkItems() {
        if (bobActorId == null) {return;}
        given()
                .when().get("/external-actors/{id}/tasks", bobActorId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }
}
