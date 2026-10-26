package io.casehub.life.app.engine.agent;

import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.life.app.engine.LifeAgent;
import io.smallrye.mutiny.Multi;
import io.casehub.worker.api.WorkerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies structured booking output and message templates through the native provider API. */
class LifeAgentProviderTest {

    private AgentProvider stubProvider;

    @BeforeEach
    void setup() {
        stubProvider = mock(AgentProvider.class);
    }

    private static Multi<AgentEvent> stubResponse(String json) {
        return Multi.createFrom().item(new AgentEvent.TextDelta(json));
    }

    private WorkerAgentInvoker bookingAgent() {
        var factory = new LifeAgentProviderFactory() {
            @Override
            public AgentProvider forAgent(LifeAgent agent) {
                return stubProvider;
            }
        };
        return factory.invokerFor(LifeAgent.HEALTH,
                "You are a healthcare appointment booking agent.",
                "Book a {{appointmentType}} appointment with provider {{provider}}.",
                BookingResult.class, java.util.function.UnaryOperator.identity());
    }

    @Test
    void execute_booking_returnsPendingAppointment() {
        // confirmed=false is correct: booking step returns PENDING.
        // confirmed=true is set by the later confirm-appointment binding.
        when(stubProvider.invoke(any(AgentSessionConfig.class))).thenReturn(stubResponse(
                "{\"appointmentId\":\"APT-123\",\"provider\":\"Dr Smith\","
                + "\"confirmed\":false,\"declined\":null,\"reason\":null}"));

        final WorkerResult<Map<String, Object>> result = bookingAgent().execute(
                Map.of("appointmentType", "GP checkup", "provider", "Dr Smith"));

        assertThat(result.output().get("appointmentId")).isEqualTo("APT-123");
        assertThat(result.output().get("provider")).isEqualTo("Dr Smith");
        assertThat(result.output().get("confirmed")).isEqualTo(false);
        assertThat(result.output().get("declined")).isNull();
    }

    @Test
    void execute_unavailableProvider_returnsDeclined() {
        when(stubProvider.invoke(any(AgentSessionConfig.class))).thenReturn(stubResponse(
                "{\"appointmentId\":null,\"provider\":\"Dr Gone\","
                + "\"confirmed\":false,\"declined\":true,\"reason\":\"Not accepting new patients\"}"));

        final WorkerResult<Map<String, Object>> result = bookingAgent().execute(
                Map.of("appointmentType", "GP checkup", "provider", "Dr Gone"));

        assertThat(result.output().get("declined")).isEqualTo(true);
        assertThat(result.output().get("reason")).isEqualTo("Not accepting new patients");
        assertThat(result.output().get("appointmentId")).isNull();
    }

    @Test
    void execute_userMessageTemplate_containsProviderName() {
        // Verifies the user message template is rendered — the rendered text is
        // what the request-aware mock in integration tests must detect.
        // Template: "Book a {{appointmentType}} appointment with provider {{provider}}."
        // With provider="unavailable" → "Book a GP appointment with provider unavailable."
        when(stubProvider.invoke(any(AgentSessionConfig.class))).thenAnswer(invocation -> {
            final AgentSessionConfig req = invocation.getArgument(0);
            final String userText = req.userPrompt();
            assertThat(req.systemPrompt()).contains("appointmentId", "calendarEventId", "toolsUsed", "boolean");
            assertThat(userText).contains("Dr Smith");
            assertThat(userText).contains("GP checkup");
            return stubResponse("{\"appointmentId\":\"APT-999\",\"provider\":\"Dr Smith\","
                    + "\"confirmed\":false,\"declined\":null,\"reason\":null}");
        });

        bookingAgent().execute(Map.of("appointmentType", "GP checkup", "provider", "Dr Smith"));
    }
    @Test
    void execute_providerFailure_isReported() {
        when(stubProvider.invoke(any())).thenReturn(
                Multi.createFrom().failure(new IllegalStateException("gateway unavailable")));
        assertThatThrownBy(() -> bookingAgent().execute(Map.of("appointmentType", "GP", "provider", "Dr Smith")))
                .hasMessageContaining("gateway unavailable");
    }

    @Test
    void execute_invalidJson_isRejected() {
        when(stubProvider.invoke(any())).thenReturn(stubResponse("not JSON"));
        assertThatThrownBy(() -> bookingAgent().execute(Map.of("appointmentType", "GP", "provider", "Dr Smith")))
                .hasMessageContaining("invalid JSON");
    }

}
