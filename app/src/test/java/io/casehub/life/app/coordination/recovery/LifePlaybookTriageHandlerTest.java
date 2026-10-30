package io.casehub.life.app.coordination.recovery;

import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.life.app.coordination.CoordinationTemplate;
import io.casehub.life.app.coordination.CoordinationTemplateCatalog;
import io.casehub.life.app.engine.LifeAgent;
import io.casehub.life.app.engine.agent.LifeAgentProviderFactory;
import io.casehub.platform.api.governance.TriageContext;
import io.casehub.platform.api.governance.TriageDecision;
import io.casehub.worker.api.WorkerResult;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class LifePlaybookTriageHandlerTest {

    private TriageExecutor triageExecutor;
    private LifeAgentProviderFactory agentFactory;
    @SuppressWarnings("unchecked")
    private final Event<PlaybookTriageEvent> triageEvent = Mockito.mock(Event.class);
    private CoordinationTemplateCatalog catalog;
    private LifePlaybookTriageHandler handler;

    @BeforeEach
    void setUp() {
        triageExecutor = Mockito.mock(TriageExecutor.class);
        agentFactory = Mockito.mock(LifeAgentProviderFactory.class);
        catalog = Mockito.mock(CoordinationTemplateCatalog.class);
        Mockito.when(catalog.all()).thenReturn(List.of());
        handler = new LifePlaybookTriageHandler(triageExecutor, agentFactory, triageEvent, catalog);
    }

    private TriageContext makeContext(int attempt, int max) {
        UUID caseId = UUID.randomUUID();
        return new TriageContext(
            caseId + ":pre-process", "worker-execution",
            attempt, max, "timeout on step 2",
            Map.of("steps", List.of(Map.of(
                "stepId", "s1", "stepName", "fetch-contacts",
                "status", "FAILED", "error", "timeout"))),
            Map.of());
    }

    @Test
    void returnsRetryWhenExecutorSaysReschedule() {
        var lifeDecision = new io.casehub.life.app.coordination.recovery.TriageDecision(
            TriageAction.RETRY_MODIFIED, "increase timeout",
            Map.of("timeout", 30), null, null, null, null);

        var invoker = Mockito.mock(WorkerAgentInvoker.class);
        Mockito.when(agentFactory.invokerFor(
            any(LifeAgent.class), anyString(), isNull(),
            eq(io.casehub.life.app.coordination.recovery.TriageDecision.class), any()))
            .thenReturn(invoker);

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> decisionMap = mapper.convertValue(lifeDecision, Map.class);
        Mockito.when(invoker.execute(any())).thenReturn(WorkerResult.of(decisionMap));

        Mockito.when(triageExecutor.apply(any(), any()))
            .thenReturn(TriageOutcome.reschedule(TriageAction.RETRY_MODIFIED, Map.of("timeout", 30), null));

        TriageDecision result = handler.evaluate(makeContext(1, 2));

        assertThat(result).isInstanceOf(TriageDecision.Retry.class);
        var retry = (TriageDecision.Retry) result;
        assertThat(retry.modifications()).containsEntry("timeout", 30);
        Mockito.verify(triageEvent).fire(any(PlaybookTriageEvent.class));
    }

    @Test
    void returnsAbandonWhenExecutorSaysFallback() {
        var lifeDecision = new io.casehub.life.app.coordination.recovery.TriageDecision(
            TriageAction.FALLBACK_MCP, "no viable playbook",
            null, null, null, null, "all templates failed");

        var invoker = Mockito.mock(WorkerAgentInvoker.class);
        Mockito.when(agentFactory.invokerFor(
            any(LifeAgent.class), anyString(), isNull(),
            eq(io.casehub.life.app.coordination.recovery.TriageDecision.class), any()))
            .thenReturn(invoker);

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> decisionMap = mapper.convertValue(lifeDecision, Map.class);
        Mockito.when(invoker.execute(any())).thenReturn(WorkerResult.of(decisionMap));

        Mockito.when(triageExecutor.apply(any(), any()))
            .thenReturn(TriageOutcome.fallback());

        TriageDecision result = handler.evaluate(makeContext(2, 2));

        assertThat(result).isInstanceOf(TriageDecision.Abandon.class);
        var abandon = (TriageDecision.Abandon) result;
        assertThat(abandon.reason()).contains("all templates failed");
    }

    @Test
    void returnsAbandonOnPlannerException() {
        Mockito.when(agentFactory.invokerFor(
            any(LifeAgent.class), anyString(), isNull(),
            eq(io.casehub.life.app.coordination.recovery.TriageDecision.class), any()))
            .thenThrow(new RuntimeException("LLM unavailable"));

        TriageDecision result = handler.evaluate(makeContext(1, 2));

        assertThat(result).isInstanceOf(TriageDecision.Abandon.class);
        var abandon = (TriageDecision.Abandon) result;
        assertThat(abandon.reason()).contains("triage planner exception");
    }
}
