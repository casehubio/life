package io.casehub.life.app;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.life.app.infrastructure.LifeChannelInitializer;
import io.casehub.platform.api.identity.ActorType;
import io.casehub.qhorus.api.message.Message;
import io.casehub.qhorus.api.message.MessageType;
import io.casehub.qhorus.api.store.MessageStore;
import io.casehub.qhorus.runtime.channel.ChannelService;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class CaseChannelQueryTest {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    @Inject io.casehub.platform.testing.FixedCurrentPrincipal fixedPrincipal;
    @Inject ChannelService channelService;
    @Inject MessageStore messageStore;

    private UUID caseTrackerId;
    private UUID emptyCaseTrackerId;

    @BeforeEach
    @Transactional
    void seed() {
        fixedPrincipal.setGroups(Set.of("household-admin"));
        LifeCaseTracker.deleteAll();
        LifeTestFixtures.seedStandardTemplates();

        UUID engineCaseId = UUID.randomUUID();
        LifeCaseTracker tracker = new LifeCaseTracker();
        tracker.caseType = "contractor-coordination";
        tracker.domain = LifeDomain.CONTRACTOR_COORDINATION;
        tracker.status = LifeCaseStatus.ACTIVE;
        tracker.engineCaseId = engineCaseId;
        tracker.createdAt = Instant.now();
        tracker.persist();
        caseTrackerId = tracker.id;

        seedMessage(LifeChannelInitializer.DELEGATION_CHANNEL, "home-agent",
                MessageType.COMMAND, "Schedule plumber visit");
        seedMessage(LifeChannelInitializer.OVERSIGHT_CHANNEL, "finance-agent",
                MessageType.COMMAND, "Approve £500 spend");

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
    void channels_returnsMessagesFromSharedChannels() {
        given()
        .when()
            .get("/life-cases/{id}/channels", caseTrackerId)
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(2)));
    }

    @Test
    void channels_messageHasExpectedFields() {
        given()
        .when()
            .get("/life-cases/{id}/channels", caseTrackerId)
        .then()
            .statusCode(200)
            .body("[0].sender", equalTo("home-agent"))
            .body("[0].messageType", equalTo("COMMAND"))
            .body("[0].content", equalTo("Schedule plumber visit"))
            .body("[0].channelName", equalTo(LifeChannelInitializer.DELEGATION_CHANNEL));
    }

    @Test
    void channels_unknownCase_returns404() {
        given()
        .when()
            .get("/life-cases/{id}/channels", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void channels_noMessages_returnsEmptyListOrSharedMessages() {
        given()
        .when()
            .get("/life-cases/{id}/channels", emptyCaseTrackerId)
        .then()
            .statusCode(200);
    }

    private void seedMessage(String channelName, String sender,
                             MessageType type, String content) {
        channelService.findByName(channelName).ifPresent(channel -> {
            Message msg = Message.builder()
                    .channelId(channel.id())
                    .sender(sender)
                    .messageType(type)
                    .actorType(ActorType.AGENT)
                    .tenancyId(TENANCY_ID)
                    .content(content)
                    .createdAt(Instant.now())
                    .build();
            messageStore.put(msg);
        });
    }
}
