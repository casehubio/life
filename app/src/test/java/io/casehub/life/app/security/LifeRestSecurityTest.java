package io.casehub.life.app.security;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;

/**
 * Security boundary tests for all REST resources.
 * Verifies @RolesAllowed enforcement: 401 for unauthenticated, 403 for wrong role.
 * Does NOT test business logic — only HTTP status codes are asserted.
 */
@QuarkusTest
class LifeRestSecurityTest {

    // ==============================
    // Unauthenticated → 401
    // ==============================

    @Test
    void unauthenticated_postLifeTasks_returns401() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_postExternalActors_returns401() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/external-actors")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_getLifeTask_returns401() {
        given()
            .when().get("/life-tasks/" + UUID.randomUUID())
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_deleteExternalActor_returns401() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID())
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_postLifeCases_returns401() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-cases")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_postOversightGate_returns401() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-oversight-gates")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_postCommitment_returns401() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks/" + UUID.randomUUID() + "/commit")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_getExternalActors_returns401() {
        given()
            .when().get("/external-actors")
            .then().statusCode(401);
    }

    @Test
    void unauthenticated_getExternalActorTasks_returns401() {
        given()
            .when().get("/external-actors/" + UUID.randomUUID() + "/tasks")
            .then().statusCode(401);
    }

    // ==============================
    // household-junior — blocked on create/mutate, allowed on GET /life-tasks/{id}
    // ==============================

    @Test
    @TestSecurity(user = "junior", roles = {"household-junior"})
    void junior_postLifeTasks_returns403() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "junior", roles = {"household-junior"})
    void junior_postLifeCases_returns403() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-cases")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "junior", roles = {"household-junior"})
    void junior_deleteExternalActor_returns403() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID())
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "junior", roles = {"household-junior"})
    void junior_postOversightGate_returns403() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-oversight-gates")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "junior", roles = {"household-junior"})
    void junior_getLifeTask_isNotForbidden() {
        // junior has operation-level access to GET /life-tasks/{id}; data filter deferred to life#41
        given()
            .when().get("/life-tasks/" + UUID.randomUUID())
            .then().statusCode(not(in(List.of(401, 403))));
    }

    // ==============================
    // household-member — blocked on admin-only, allowed on member endpoints
    // ==============================

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_deleteExternalActor_returns403() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID())
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_putExternalActor_returns403() {
        given().contentType(ContentType.JSON).body("{}")
            .when().put("/external-actors/" + UUID.randomUUID())
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_eraseExternalActorPersonalData_returns403() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID() + "/personal-data")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_postLifeTasks_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks")
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_postLifeCases_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-cases")
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_postOversightGate_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-oversight-gates")
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "member", roles = {"household-member"})
    void member_postCommitment_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks/" + UUID.randomUUID() + "/commit")
            .then().statusCode(not(in(List.of(401, 403))));
    }

    // ==============================
    // household-admin — access to all endpoints
    // ==============================

    @Test
    @TestSecurity(user = "admin", roles = {"household-admin"})
    void admin_deleteExternalActor_isNotForbidden() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID())
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"household-admin"})
    void admin_eraseExternalActorPersonalData_isNotForbidden() {
        given()
            .when().delete("/external-actors/" + UUID.randomUUID() + "/personal-data")
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"household-admin"})
    void admin_putExternalActor_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().put("/external-actors/" + UUID.randomUUID())
            .then().statusCode(not(in(List.of(401, 403))));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"household-admin"})
    void admin_postLifeTasks_isNotForbidden() {
        given().contentType(ContentType.JSON).body("{}")
            .when().post("/life-tasks")
            .then().statusCode(not(in(List.of(401, 403))));
    }
}
