package io.casehub.life.app.engine.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import io.casehub.life.app.engine.LifeAgent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Map;

@Alternative
@Priority(10)
@ApplicationScoped
public class TestLifeAgentProviderFactory extends LifeAgentProviderFactory {

    private static final Map<String, String> RESPONSES = Map.ofEntries(
            // --- Health domain (health-agent) ---
            Map.entry("healthcare appointment booking",
                      "{\"appointmentId\":\"APT-MOCK\",\"provider\":\"Dr Smith\","
                      + "\"confirmed\":false,\"declined\":null,\"reason\":null,"
                      + "\"contactId\":\"CTK-MOCK-APT\","
                      + "\"calendarEventId\":\"evt_MOCK_APT\","
                      + "\"toolsUsed\":[\"search_contacts\",\"calendar_create_event\"]}"),
            Map.entry("find an alternative",
                      "{\"alternativeFound\":true,\"appointmentId\":\"APT-ALT-MOCK\","
                      + "\"provider\":\"Dr Alternative\",\"confirmed\":false,"
                      + "\"contactId\":\"CTK-MOCK-ALT\","
                      + "\"calendarEventId\":\"evt_MOCK_ALT\","
                      + "\"toolsUsed\":[\"search_contacts\",\"list_contacts\",\"calendar_list_events\",\"calendar_create_event\"]}"),
            Map.entry("send appointment confirmation",
                      "{\"confirmed\":true,\"reminderSent\":true,"
                      + "\"calendarEventId\":\"evt_MOCK_REM\",\"notificationMessageId\":\"msg_MOCK_CONF\","
                      + "\"toolsUsed\":[\"calendar_create_event\",\"send_chat\"]}"),
            Map.entry("pre-visit preparation",
                      "{\"checklistSent\":true,\"instructions\":\"Bring ID, insurance card\","
                      + "\"emailMessageId\":\"eml_MOCK_PREP\","
                      + "\"notificationMessageId\":\"msg_MOCK_PREP\","
                      + "\"toolsUsed\":[\"send_email\",\"send_chat\"]}"),
            Map.entry("record health decision",
                      "{\"recorded\":true,\"ledgerEntryId\":\"LEDGER-MOCK\","
                      + "\"toolsUsed\":[]}"),
            Map.entry("assess care needs",
                      "{\"careLevel\":\"moderate\",\"recommendedFrequency\":\"weekly\","
                      + "\"specialRequirements\":[\"mobility support\"],"
                      + "\"contactId\":\"CTK-MOCK-CARE\","
                      + "\"sensorReadings\":[\"movement:detected\",\"temp:36.6\"],"
                      + "\"toolsUsed\":[\"search_contacts\",\"iot_get_state\"]}"),
            Map.entry("create a care plan",
                      "{\"schedule\":[\"Mon 9am\",\"Wed 2pm\"],\"duration\":\"2 hours\","
                      + "\"tasks\":[\"medication\",\"mobility exercises\"],"
                      + "\"contactId\":\"CTK-MOCK-CPLAN\","
                      + "\"calendarEventId\":\"evt_MOCK_CARE\","
                      + "\"toolsUsed\":[\"search_contacts\",\"calendar_create_event\"]}"),
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
                      + "\"contactId\":\"CTK-MOCK-001\","
                      + "\"notificationMessageId\":\"msg_MOCK_001\","
                      + "\"toolsUsed\":[\"search_contacts\",\"list_contacts\",\"send_chat\"]}"),
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
                      + "\"emailMessageId\":\"eml_MOCK_003\","
                      + "\"notificationMessageId\":\"msg_MOCK_003\","
                      + "\"toolsUsed\":[\"send_email\",\"send_chat\"]}"),
            Map.entry("request a quote",
                      "{\"quoteRequested\":true,\"channel\":\"life/contractor/mock\","
                      + "\"deadlinePassed\":false,\"contactId\":\"CTK-MOCK-004\","
                      + "\"notificationMessageId\":\"msg_MOCK_004\","
                      + "\"toolsUsed\":[\"search_contacts\",\"send_chat\"]}"),
            Map.entry("escalate an overdue",
                      "{\"escalated\":true,\"reminderSent\":true,"
                      + "\"notificationMessageId\":\"msg_MOCK_005\","
                      + "\"toolsUsed\":[\"send_chat\"]}"),
            Map.entry("process a received quote",
                      "{\"quoteAmount\":500,\"contractor\":\"ABC Plumbing\","
                      + "\"validUntil\":\"2026-07-30\",\"toolsUsed\":[]}"),
            Map.entry("monitor an active contractor job",
                      "{\"progress\":\"50% complete\",\"estimatedCompletion\":\"2026-07-20\","
                      + "\"contactId\":\"CTK-MOCK-006\","
                      + "\"notificationMessageId\":\"msg_MOCK_006\","
                      + "\"toolsUsed\":[\"search_contacts\",\"send_chat\"]}"),
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
                      + "\"ledgerEntryId\":\"LEDGER-MOCK\",\"emailMessageId\":\"eml_MOCK_RPT\","
                      + "\"notificationMessageId\":\"msg_MOCK_RPT\","
                      + "\"toolsUsed\":[\"send_email\",\"send_chat\"]}"),

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
                      + "\"calendarEventId\":\"evt_MOCK_ITIN\",\"emailMessageId\":\"eml_MOCK_ITIN\","
                      + "\"notificationMessageId\":\"msg_MOCK_TRAV\","
                      + "\"toolsUsed\":[\"calendar_create_event\",\"send_email\",\"send_chat\"]}")
    );

    @SuppressWarnings("unused")
    protected TestLifeAgentProviderFactory() {
        super();
    }

    @Override
    public AgentProvider forAgent(LifeAgent agent) {
        return new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig request) {
                String sysPrompt = request.systemPrompt().toLowerCase();
                boolean decline = request.userPrompt().toLowerCase().contains("unavailable");
                if (decline && sysPrompt.contains("appointment booking")) {
                    return respond("{\"appointmentId\":null,\"provider\":\"Dr Gone\","
                            + "\"confirmed\":false,\"declined\":true,"
                            + "\"reason\":\"Provider unavailable\","
                            + "\"contactId\":null,"
                            + "\"calendarEventId\":null,"
                            + "\"toolsUsed\":[\"search_contacts\"]}");
                }

                for (var entry : RESPONSES.entrySet()) {
                    if (sysPrompt.contains(entry.getKey())) {
                        return respond(entry.getValue());
                    }
                }

                return respond("{\"ok\":true}");
            }

            @Override
            public AgentSession openSession(AgentSessionInit init) {
                throw new UnsupportedOperationException();
            }

            private Multi<AgentEvent> respond(String json) {
                return Multi.createFrom().item(new AgentEvent.TextDelta(json));
            }
        };
    }
}
