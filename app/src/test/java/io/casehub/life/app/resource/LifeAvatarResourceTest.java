package io.casehub.life.app.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class LifeAvatarResourceTest {

    @Test
    @TestSecurity(user = "test-member", roles = "household-member")
    void postMessageReturns200() {
        given()
            .contentType("text/plain")
            .body("Hello, what tasks do I have today?")
        .when()
            .post("/avatar/message")
        .then()
            .statusCode(200);
    }

    @Test
    void postMessageWithoutAuthReturns401() {
        given()
            .contentType("text/plain")
            .body("Hello")
        .when()
            .post("/avatar/message")
        .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-admin", roles = "household-admin")
    void adminCanAccessAvatar() {
        given()
            .contentType("text/plain")
            .body("Hello")
        .when()
            .post("/avatar/message")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "test-junior", roles = "household-junior")
    void juniorCanAccessAvatar() {
        given()
            .contentType("text/plain")
            .body("Hello")
        .when()
            .post("/avatar/message")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "test-member", roles = "household-member")
    void deleteSessionReturns204() {
        given()
        .when()
            .delete("/avatar/session")
        .then()
            .statusCode(204);
    }
}
