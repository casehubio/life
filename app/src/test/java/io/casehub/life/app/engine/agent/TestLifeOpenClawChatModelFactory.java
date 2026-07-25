package io.casehub.life.app.engine.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.api.model.ai.ChatModelProvider;
import io.casehub.api.model.ai.ModelType;
import io.casehub.life.app.engine.LifeAgent;
import io.casehub.openclaw.casehub.DirectCallBridge;
import io.casehub.openclaw.client.OpenClawClientConfig;
import io.casehub.openclaw.client.OpenClawHookClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Map;

/**
 * Test replacement for {@link LifeOpenClawChatModelFactory}.
 *
 * <p>Returns a {@link ChatModel} that matches system prompt content against a canned
 * response map. No OpenClaw HTTP calls are made.
 *
 * <p>Registered in {@code quarkus.arc.selected-alternatives} in test config.
 *
 * <p>This is an {@code @Alternative} bean that extends the production factory. The
 * production factory constructor requires {@link DirectCallBridge}, {@link OpenClawHookClient},
 * and {@link OpenClawClientConfig} — beans which may not be available in tests because
 * the OpenClaw REST client (openclaw-gateway) points at a non-existent URL.
 * The test factory's protected no-arg constructor passes nulls to the super constructor —
 * it overrides {@link #forAgent(String)} entirely and never touches the bridge/hookClient.
 *
 * <p>RESPONSE map entries are keyed by system prompt substrings (case-insensitive).
 * Decline path: if the user message contains "unavailable" and the system prompt contains
 * "appointment booking", returns a declined booking response.
 *
 * <p>Refs casehubio/life#38 — Phase 2 test infrastructure.
 */
@Alternative
@Priority(10)
@ApplicationScoped
public class TestLifeOpenClawChatModelFactory extends LifeOpenClawChatModelFactory {

    private static final Map<String, String> RESPONSES = Map.ofEntries(
            // --- Health domain (health-agent) ---
            Map.entry("healthcare appointment booking",
                      "{\"appointmentId\":\"APT-MOCK\",\"provider\":\"Dr Smith\","
                      + "\"confirmed\":false,\"declined\":null,\"reason\":null,"
                      + "\"calendarEventId\":\"evt_MOCK_APT\","
                      + "\"toolsUsed\":[\"calendar_create_event\"]}"),
            Map.entry("find an alternative",
                      "{\"alternativeFound\":true,\"appointmentId\":\"APT-ALT-MOCK\","
                      + "\"provider\":\"Dr Alternative\",\"confirmed\":false,"
                      + "\"calendarEventId\":\"evt_MOCK_ALT\","
                      + "\"toolsUsed\":[\"calendar_list_events\",\"calendar_create_event\"]}"),
            Map.entry("send appointment confirmation",
                      "{\"confirmed\":true,\"reminderSent\":true,"
                      + "\"calendarEventId\":\"evt_MOCK_REM\",\"notificationMessageId\":\"msg_MOCK_CONF\","
                      + "\"toolsUsed\":[\"calendar_create_event\",\"send_chat\"]}"),
            Map.entry("pre-visit preparation",
                      "{\"checklistSent\":true,\"instructions\":\"Bring ID, insurance card\","
                      + "\"notificationMessageId\":\"msg_MOCK_PREP\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("record health decision",
                      "{\"recorded\":true,\"ledgerEntryId\":\"LEDGER-MOCK\","
                      + "\"toolsUsed\":[]}"),
            Map.entry("assess care needs",
                      "{\"careLevel\":\"moderate\",\"recommendedFrequency\":\"weekly\","
                      + "\"specialRequirements\":[\"mobility support\"],"
                      + "\"sensorReadings\":[\"movement:detected\",\"temp:36.6\"],"
                      + "\"toolsUsed\":[\"iot_get_state\"]}"),
            Map.entry("create a care plan",
                      "{\"schedule\":[\"Mon 9am\",\"Wed 2pm\"],\"duration\":\"2 hours\","
                      + "\"tasks\":[\"medication\",\"mobility exercises\"],"
                      + "\"calendarEventId\":\"evt_MOCK_CARE\","
                      + "\"toolsUsed\":[\"calendar_create_event\"]}"),
            Map.entry("periodic health check",
                      "{\"reviewed\":true,\"healthConcern\":false,\"notes\":\"Stable condition\","
                      + "\"sensorReadings\":[\"temp:36.5\",\"movement:normal\"],"
                      + "\"notificationMessageId\":null,"
                      + "\"toolsUsed\":[\"iot_get_state\"]}"),
            Map.entry("assess patient condition",
                      "{\"vitalSigns\":{\"bp\":\"120/80\",\"hr\":72,\"temp\":36.6},"
                      + "\"mobility\":\"assisted\",\"cognition\":\"alert\","
                      + "\"sensorReadings\":[\"movement:limited\",\"temp:22.1\"],"
                      + "\"toolsUsed\":[\"iot_get_state\"]}"),
            Map.entry("provide care",
                      "{\"tasksCompleted\":[\"medication\",\"mobility\"],\"duration\":\"90 min\","
                      + "\"observations\":\"Patient cooperative\","
                      + "\"notificationMessageId\":\"msg_MOCK_CARE\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),

            // --- Home domain (home-agent) ---
            Map.entry("schedule a property inspection",
                      "{\"inspected\":true,\"condition\":\"good\",\"inspectionDate\":\"2026-07-01\","
                      + "\"calendarEventId\":\"evt_MOCK_001\",\"sensorReadings\":[\"temp:21.3\",\"humidity:45\"],"
                      + "\"toolsUsed\":[\"iot_get_state\",\"calendar_create_event\"]}"),
            Map.entry("gather contractor quotes",
                      "{\"quoteCount\":2,\"quotes\":[{\"contractor\":\"ABC\",\"amount\":500,"
                      + "\"available\":true},{\"contractor\":\"DEF\",\"amount\":650,\"available\":true}],"
                      + "\"notificationMessageId\":\"msg_MOCK_001\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("issue a commitment to the selected contractor",
                      "{\"commitmentIssued\":true,\"channel\":\"life/contractor/mock\","
                      + "\"notificationMessageId\":\"msg_MOCK_002\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("monitor job progress",
                      "{\"progress\":\"75% complete\",\"estimatedCompletion\":\"2026-07-15\","
                      + "\"sensorReadings\":[\"temp:22.0\"],\"notificationMessageId\":null,"
                      + "\"toolsUsed\":[\"iot_get_state\"]}"),
            Map.entry("record job completion",
                      "{\"recorded\":true,\"ledgerEntryId\":\"LEDGER-MOCK\","
                      + "\"notificationMessageId\":\"msg_MOCK_003\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("request a quote",
                      "{\"quoteRequested\":true,\"channel\":\"life/contractor/mock\","
                      + "\"deadlinePassed\":false,\"notificationMessageId\":\"msg_MOCK_004\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("escalate an overdue",
                      "{\"escalated\":true,\"reminderSent\":true,"
                      + "\"notificationMessageId\":\"msg_MOCK_005\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("process a received quote",
                      "{\"quoteAmount\":500,\"contractor\":\"ABC Plumbing\","
                      + "\"validUntil\":\"2026-07-30\",\"toolsUsed\":[]}"),
            Map.entry("monitor an active contractor job",
                      "{\"progress\":\"50% complete\",\"estimatedCompletion\":\"2026-07-20\","
                      + "\"notificationMessageId\":\"msg_MOCK_006\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("record a contractor payment",
                      "{\"paymentRecorded\":true,\"amount\":500,\"ledgerEntryId\":\"LEDGER-MOCK\","
                      + "\"crossCaseSignal\":\"payment-complete\","
                      + "\"notificationMessageId\":\"msg_MOCK_007\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),

            // --- Finance domain (finance-agent) ---
            Map.entry("gather financial data",
                      "{\"totalSpend\":5000,\"budgetLimit\":4500,"
                      + "\"categories\":[\"groceries\",\"utilities\",\"contractor\"],"
                      + "\"transactionSummary\":{\"totalTransactions\":42,\"period\":\"2026-07\"},"
                      + "\"toolsUsed\":[\"bank_get_transactions\",\"bank_get_balances\"]}"),
            Map.entry("analyse spending anomalies",
                      "{\"hasAnomalies\":true,\"anomalyDetails\":\"Spending exceeded budget by $500 (11%)\","
                      + "\"transactionSummary\":{\"flaggedCount\":3},"
                      + "\"toolsUsed\":[\"bank_get_transactions\"]}"),
            Map.entry("escalate anomalies",
                      "{\"escalationSent\":true,\"channel\":\"life/oversight\","
                      + "\"notificationMessageId\":\"msg_MOCK_ESC\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("process oversight response",
                      "{\"approved\":true,\"comments\":\"Approved by household admin\","
                      + "\"toolsUsed\":[]}"),
            Map.entry("produce a monthly financial report",
                      "{\"reportGenerated\":true,\"summary\":\"Within budget\","
                      + "\"ledgerEntryId\":\"LEDGER-MOCK\","
                      + "\"notificationMessageId\":\"msg_MOCK_RPT\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),

            // --- Travel domain (travel-agent) ---
            Map.entry("research destination options",
                      "{\"options\":[{\"name\":\"Paris\",\"cost\":1200,\"rating\":\"4.5\"},"
                      + "{\"name\":\"Barcelona\",\"cost\":900,\"rating\":\"4.3\"}],"
                      + "\"toolsUsed\":[\"calendar_list_events\"]}"),
            Map.entry("search for flights",
                      "{\"flights\":[{\"airline\":\"BA\",\"price\":450,\"stops\":0},"
                      + "{\"airline\":\"RY\",\"price\":280,\"stops\":1}],"
                      + "\"toolsUsed\":[]}"),
            Map.entry("search for hotels",
                      "{\"hotels\":[{\"name\":\"Grand Hotel\",\"price\":120,\"rating\":4.5},"
                      + "{\"name\":\"Budget Inn\",\"price\":60,\"rating\":3.0}],"
                      + "\"toolsUsed\":[]}"),
            Map.entry("assess the total travel budget",
                      "{\"totalCost\":3500,\"requiresApproval\":true,\"isHighValue\":false,"
                      + "\"toolsUsed\":[]}"),
            Map.entry("book the selected flights and hotels",
                      "{\"bookingRef\":\"BK-MOCK\",\"status\":\"confirmed\","
                      + "\"declined\":null,\"reason\":null,"
                      + "\"calendarEventId\":\"evt_MOCK_TRAVEL\","
                      + "\"toolsUsed\":[\"calendar_create_event\"]}"),
            Map.entry("rebook after a declined",
                      "{\"bookingRef\":\"BK-REBK-MOCK\",\"status\":\"confirmed\","
                      + "\"alternativeDates\":true,"
                      + "\"calendarEventId\":\"evt_MOCK_REBK\","
                      + "\"toolsUsed\":[\"calendar_create_event\"]}"),
            Map.entry("confirm the travel itinerary",
                      "{\"confirmed\":true,\"itinerarySent\":true,"
                      + "\"confirmationRef\":\"CONF-MOCK\","
                      + "\"calendarEventId\":\"evt_MOCK_ITIN\","
                      + "\"notificationMessageId\":\"msg_MOCK_TRAV\","
                      + "\"toolsUsed\":[\"calendar_create_event\",\"send_chat\"]}")
                                                                      );

    @SuppressWarnings("unused")
    protected TestLifeOpenClawChatModelFactory() {
        super();
    }

    @Override
    public ChatModelProvider forAgent(LifeAgent agent) {
        return new ChatModelProvider() {
            @Override
            public ModelType type() {
                return ModelType.OPENAI;
            }

            @Override
            public ChatModel get() {
                return new TestChatModel();
            }
        };
    }

    private static final class TestChatModel implements ChatModel {
        @Override
        public ChatResponse doChat(ChatRequest request) {
            String sysPrompt = request.messages().stream()
                    .filter(m -> m instanceof SystemMessage)
                    .map(m -> ((SystemMessage) m).text().toLowerCase())
                    .findFirst()
                    .orElse("");

            // Match decline path for appointment booking
            boolean decline = request.messages().stream()
                    .filter(m -> m instanceof dev.langchain4j.data.message.UserMessage)
                    .map(m -> ((dev.langchain4j.data.message.UserMessage) m).singleText())
                    .findFirst()
                    .map(t -> t.toLowerCase().contains("unavailable"))
                    .orElse(false);
            if (decline && sysPrompt.contains("appointment booking")) {
                return respond("{\"appointmentId\":null,\"provider\":\"Dr Gone\","
                        + "\"confirmed\":false,\"declined\":true,"
                        + "\"reason\":\"Provider unavailable\","
                        + "\"calendarEventId\":null,"
                        + "\"toolsUsed\":[]}");
            }

            for (var entry : RESPONSES.entrySet()) {
                if (sysPrompt.contains(entry.getKey())) {
                    return respond(entry.getValue());
                }
            }

            return respond("{\"ok\":true}");
        }

        private static ChatResponse respond(String json) {
            return ChatResponse.builder()
                    .aiMessage(new AiMessage(json))
                    .build();
        }
    }
}
