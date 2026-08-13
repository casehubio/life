package io.casehub.life.app.resource;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.app.LifeTestFixtures;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.runtime.model.WorkItemEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {HouseholdGroups.ADMIN})
class DashboardResourceTest {

    @BeforeEach
    @Transactional
    void seedTemplates() {
        LifeTaskContext.deleteAll();
        WorkItemEntity.deleteAll();
        LifeTestFixtures.seedStandardTemplates();
    }

    @Test
    void briefing_returns_greeting_and_items() {
        given()
                .contentType("application/json")
                .body("""
                        {"templateRef":"health-appointment","title":"Overdue GP call",
                         "deadline":"%s"}
                        """.formatted(Instant.now().minus(1, ChronoUnit.HOURS)))
                .when().post("/life-tasks")
                .then().statusCode(201);

        given()
                .when().get("/dashboard/briefing")
                .then().statusCode(200)
                .body("greeting", startsWith("Good"))
                .body("actionCount", greaterThanOrEqualTo(1))
                .body("items.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void briefing_empty_when_no_tasks() {
        given()
                .when().get("/dashboard/briefing")
                .then().statusCode(200)
                .body("greeting", startsWith("Good"))
                .body("actionCount", org.hamcrest.Matchers.equalTo(0));
    }

    @Test
    @TestSecurity(user = "junior", roles = {HouseholdGroups.JUNIOR})
    void briefing_accessible_to_junior() {
        given()
                .when().get("/dashboard/briefing")
                .then().statusCode(200);
    }
}
