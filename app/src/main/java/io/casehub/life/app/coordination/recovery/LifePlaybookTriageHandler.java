package io.casehub.life.app.coordination.recovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.life.app.coordination.CoordinationTemplateCatalog;
import io.casehub.life.app.engine.LifeAgent;
import io.casehub.life.app.engine.agent.LifeAgentProviderFactory;
import io.casehub.platform.api.governance.TriageContext;
import io.casehub.platform.api.governance.TriageDecision;
import io.casehub.platform.api.governance.TriageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.jboss.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LifePlaybookTriageHandler implements TriageHandler {

    private static final Logger LOG = Logger.getLogger(LifePlaybookTriageHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final TriageExecutor triageExecutor;
    private final LifeAgentProviderFactory agentFactory;
    private final Event<PlaybookTriageEvent> triageEvent;
    private final CoordinationTemplateCatalog catalog;

    public LifePlaybookTriageHandler(
            TriageExecutor triageExecutor,
            LifeAgentProviderFactory agentFactory,
            Event<PlaybookTriageEvent> triageEvent,
            CoordinationTemplateCatalog catalog) {
        this.triageExecutor = triageExecutor;
        this.agentFactory = agentFactory;
        this.triageEvent = triageEvent;
        this.catalog = catalog;
    }

    @Override
    public TriageDecision evaluate(TriageContext context) {
        io.casehub.life.app.coordination.recovery.TriageDecision lifeDecision;
        try {
            lifeDecision = invokeTriagePlanner(context);
        } catch (Exception e) {
            LOG.warn("Triage planner invocation failed", e);
            return new TriageDecision.Abandon("triage planner exception: " + e.getMessage());
        }

        TriageOutcome outcome = triageExecutor.apply(lifeDecision, extractCaseId(context));

        fireTriageEvent(context, lifeDecision);

        if (outcome.shouldReschedule()) {
            return new TriageDecision.Retry(outcome.modifiedParams());
        }
        return new TriageDecision.Abandon(
            lifeDecision.fallbackReason() != null
                ? lifeDecision.fallbackReason()
                : "triage action: " + lifeDecision.action());
    }

    private io.casehub.life.app.coordination.recovery.TriageDecision invokeTriagePlanner(
            TriageContext context) {
        String templateListing = catalog.all().stream()
            .map(t -> t.getName() + " (" + t.getDomain() + ")")
            .collect(Collectors.joining(", "));

        String systemPrompt = buildTriagePrompt(context, templateListing);
        WorkerAgentInvoker invoker = agentFactory.invokerFor(
            LifeAgent.HOME, systemPrompt, null,
            io.casehub.life.app.coordination.recovery.TriageDecision.class,
            input -> input);
        var result = invoker.execute(Map.of(
            "operationId", context.operationId(),
            "failureSummary", context.failureSummary(),
            "failureDetail", context.failureDetail(),
            "attemptNumber", context.attemptNumber(),
            "maxAttempts", context.maxAttempts()));
        return MAPPER.convertValue(result.output(),
            io.casehub.life.app.coordination.recovery.TriageDecision.class);
    }

    @SuppressWarnings("unchecked")
    private String buildTriagePrompt(TriageContext context, String templateListing) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are triaging a playbook failure. Attempt ")
            .append(context.attemptNumber()).append(" of ").append(context.maxAttempts()).append(".\n\n");
        prompt.append("Failure: ").append(context.failureSummary()).append("\n");

        Map<String, Object> detail = context.failureDetail();
        if (detail.containsKey("steps")) {
            prompt.append("Step results:\n");
            Object stepsObj = detail.get("steps");
            if (stepsObj instanceof List<?> steps) {
                for (Object s : steps) {
                    if (s instanceof Map<?, ?> stepMap) {
                        prompt.append("  - ").append(stepMap.get("stepName"))
                            .append(": ").append(stepMap.get("status"));
                        Object err = stepMap.get("error");
                        if (err != null && !err.toString().isEmpty()) {
                            prompt.append(" (").append(err).append(")");
                        }
                        prompt.append("\n");
                    }
                }
            }
        }

        prompt.append("\nAvailable templates: ").append(templateListing).append("\n\n");
        prompt.append("Return a TriageDecision JSON with one of:\n");
        prompt.append("- RETRY_MODIFIED: same playbook, adjust params (early attempts preferred)\n");
        prompt.append("- SWITCH_PLAYBOOK: select a different template by name\n");
        prompt.append("- GENERATE_FIX: produce fixed YAML (later attempts preferred)\n");
        prompt.append("- FALLBACK_MCP: abandon playbooks, use MCP tools directly\n");

        return prompt.toString();
    }

    private void fireTriageEvent(TriageContext context,
                                  io.casehub.life.app.coordination.recovery.TriageDecision decision) {
        triageEvent.fire(new PlaybookTriageEvent(
            extractCaseId(context),
            null,
            context.operationId(),
            null,
            null,
            decision.action(),
            decision.reasoning(),
            context.attemptNumber(),
            null,
            Map.of()));
    }

    private UUID extractCaseId(TriageContext context) {
        String opId = context.operationId();
        int colonIdx = opId.indexOf(':');
        if (colonIdx > 0) {
            try {
                return UUID.fromString(opId.substring(0, colonIdx));
            } catch (IllegalArgumentException e) {
                return UUID.randomUUID();
            }
        }
        return UUID.randomUUID();
    }
}
