package io.casehub.life.app.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class LifeCalendarResourceTest {

    @Test
    @TestSecurity(user = "test-user", roles = "household-member")
    void listEventsReturnsEmptyListWhenNoEvents() {
        given()
            .queryParam("from", "2026-11-01T00:00:00Z")
            .queryParam("to", "2026-11-08T00:00:00Z")
        .when()
            .get("/life-calendar/events")
        .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    @Test
    void listEventsRequiresAuth() {
        given()
            .queryParam("from", "2026-11-01T00:00:00Z")
            .queryParam("to", "2026-11-08T00:00:00Z")
        .when()
            .get("/life-calendar/events")
        .then()
            .statusCode(401);
    }
}
