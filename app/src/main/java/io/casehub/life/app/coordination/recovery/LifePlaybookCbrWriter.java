package io.casehub.life.app.coordination.recovery;

import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.platform.api.path.Path;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class LifePlaybookCbrWriter {

    private static final Logger LOG = Logger.getLogger(LifePlaybookCbrWriter.class);
    static final String TENANT_ID = "life-personal";

    private final CbrRecordStore cbrStore;

    public LifePlaybookCbrWriter(CbrRecordStore cbrStore) {
        this.cbrStore = cbrStore;
    }

    void onTriageEvent(@Observes PlaybookTriageEvent event) {
        try {
            Map<String, FeatureValue> features = extractFeatures(event);

            String domain = event.caseContext() != null
                ? String.valueOf(event.caseContext().getOrDefault("domain", "unknown"))
                : "unknown";

            String problem = "Playbook failure for worker " + event.workerName();
            if (event.templateName() != null) {
                problem += " using template " + event.templateName();
            }

            String solution = event.triageAction() != TriageAction.FALLBACK_MCP
                ? "Resolved via " + event.triageAction()
                : "Unresolved — fell back to MCP tools";

            String outcome = event.triageAction() != TriageAction.FALLBACK_MCP
                ? "resolved" : "unresolved";

            CbrPlanRecord record = new CbrPlanRecord(
                problem, solution, outcome, null,
                features, List.of(), null, null);

            String cbrDomain = "casehubio/life/playbook-triage/" + domain.toLowerCase();
            cbrStore.store(
                record, "playbook-triage",
                event.workerName(),
                new MemoryDomain(cbrDomain),
                TENANT_ID,
                event.caseId() != null ? event.caseId().toString() : "unknown",
                Path.parse(cbrDomain));

            LOG.debugf("CBR record stored for playbook triage: worker=%s action=%s",
                event.workerName(), event.triageAction());
        } catch (Exception e) {
            LOG.warnf(e, "CBR retention failed for playbook triage event — proceeding without recording");
        }
    }

    private Map<String, FeatureValue> extractFeatures(PlaybookTriageEvent event) {
        Map<String, FeatureValue> features = new LinkedHashMap<>();
        features.put("templateName",
            new FeatureValue.StringVal(
                event.templateName() != null ? event.templateName() : "unknown"));
        features.put("triageAction",
            new FeatureValue.StringVal(event.triageAction().name()));
        features.put("triageRound",
            new FeatureValue.NumberVal((double) event.triageRound()));

        if (event.executionResult() != null) {
            var steps = event.executionResult().steps();
            long failedCount = steps.stream()
                .filter(s -> s.status() == StepStatus.FAILED).count();
            features.put("failedStepCount", new FeatureValue.NumberVal((double) failedCount));

            String failedNames = steps.stream()
                .filter(s -> s.status() == StepStatus.FAILED)
                .map(StepResult::stepName)
                .collect(Collectors.joining(","));
            if (!failedNames.isEmpty()) {
                features.put("failedStepNames",
                    new FeatureValue.StringVal(failedNames));
            }

            String firstError = steps.stream()
                .filter(s -> s.status() == StepStatus.FAILED && s.error() != null)
                .map(StepResult::error)
                .findFirst().orElse("unknown");
            features.put("connectorErrorType",
                new FeatureValue.StringVal(categorizeError(firstError)));
        }
        return features;
    }

    static String categorizeError(String error) {
        if (error == null) return "unknown";
        String lower = error.toLowerCase();
        if (lower.contains("timeout")) return "timeout";
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized"))
            return "auth";
        if (lower.contains("404") || lower.contains("not found")) return "not-found";
        if (lower.contains("500") || lower.contains("internal server")) return "server-error";
        return "other";
    }
}
