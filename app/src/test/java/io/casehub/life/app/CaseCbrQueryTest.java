package io.casehub.life.app;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.entity.LifeCaseTracker;
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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class CaseCbrQueryTest {

    @Inject io.casehub.platform.testing.FixedCurrentPrincipal fixedPrincipal;

    private UUID caseWithCbr;
    private UUID caseWithoutCbr;

    @BeforeEach
    @Transactional
    void seed() {
        fixedPrincipal.setGroups(Set.of("household-admin"));
        LifeCaseTracker.deleteAll();
        LifeTestFixtures.seedStandardTemplates();

        LifeCaseTracker withCbr = new LifeCaseTracker();
        withCbr.caseType = "contractor-coordination";
        withCbr.domain = LifeDomain.CONTRACTOR_COORDINATION;
        withCbr.status = LifeCaseStatus.ACTIVE;
        withCbr.engineCaseId = UUID.randomUUID();
        withCbr.createdAt = Instant.now();
        withCbr.cbrPrecedentsJson = """
                [
                  {"caseId":"prev-case-001","similarity":0.92,"outcome":"COMPLETED","resolutionTime":"2026-07-15T10:30:00Z"},
                  {"caseId":"prev-case-002","similarity":0.78,"outcome":"COMPLETED","resolutionTime":"2026-06-20T14:00:00Z"},
                  {"caseId":"prev-case-003","similarity":0.65,"outcome":"FAILED","resolutionTime":null}
                ]
                """;
        withCbr.persist();
        caseWithCbr = withCbr.id;

        LifeCaseTracker withoutCbr = new LifeCaseTracker();
        withoutCbr.caseType = "travel-plan";
        withoutCbr.domain = LifeDomain.TRAVEL;
        withoutCbr.status = LifeCaseStatus.ACTIVE;
        withoutCbr.engineCaseId = UUID.randomUUID();
        withoutCbr.createdAt = Instant.now();
        withoutCbr.persist();
        caseWithoutCbr = withoutCbr.id;
    }

    @AfterEach
    void resetPrincipal() {
        fixedPrincipal.reset();
    }

    @Test
    void cbr_returnsPrecedents() {
        given()
        .when()
            .get("/life-cases/{id}/cbr", caseWithCbr)
        .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("[0].caseId", equalTo("prev-case-001"))
            .body("[0].similarity", equalTo(0.92f))
            .body("[0].outcome", equalTo("COMPLETED"))
            .body("[0].resolutionTime", equalTo("2026-07-15T10:30:00Z"))
            .body("[2].outcome", equalTo("FAILED"));
    }

    @Test
    void cbr_noPrecedents_returnsEmptyList() {
        given()
        .when()
            .get("/life-cases/{id}/cbr", caseWithoutCbr)
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    void cbr_unknownCase_returns404() {
        given()
        .when()
            .get("/life-cases/{id}/cbr", UUID.randomUUID())
        .then()
            .statusCode(404);
    }
}
