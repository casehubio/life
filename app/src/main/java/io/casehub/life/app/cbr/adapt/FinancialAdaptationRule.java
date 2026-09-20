package io.casehub.life.app.cbr.adapt;

import io.casehub.life.app.cbr.LifeAdaptationRule;
import io.casehub.neocortex.memory.cbr.AdaptationAction;
import io.casehub.neocortex.memory.cbr.AdaptedStep;
import io.casehub.neocortex.memory.cbr.CbrMatch;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrPlanStep;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class FinancialAdaptationRule implements LifeAdaptationRule {

    private static final Set<String> CAPABILITIES = Set.of(
            "gather-data", "analyse-anomalies", "escalate-anomalies",
            "oversight-response", "produce-report", "anomaly-sentinel");

    @Override
    public String caseType() {
        return "financial-review";
    }

    @Override
    public Set<String> knownCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public List<AdaptedStep> adapt(CbrMatch<CbrPlanRecord> retrieved,
                                   Map<String, FeatureValue> currentFeatures) {
        CbrPlanRecord past = retrieved.cbrRecord();
        double currentAmount = numericFeature(currentFeatures, "amount");
        double pastAmount = numericFeature(past.features(), "amount");
        boolean pastHadEscalation = past.cbrPlanStep().stream()
                .anyMatch(t -> t.stepOutcome() != null
                        && t.stepOutcome().toLowerCase().contains("escalat"));

        List<AdaptedStep> steps = new ArrayList<>();
        for (CbrPlanStep trace : past.cbrPlanStep()) {
            Map<String, Object> params = new LinkedHashMap<>(trace.parameters());
            int priority = trace.priority();
            AdaptationAction action = AdaptationAction.RETAINED;
            String reason = null;

            if (currentAmount > 0 && pastAmount > 0 && currentAmount > pastAmount * 1.5) {
                if ("escalate-anomalies".equals(trace.capabilityName())
                        || "oversight-response".equals(trace.capabilityName())) {
                    priority = Math.min(priority + 2, 10);
                    action = AdaptationAction.BOOSTED;
                    reason = "Current amount (%.0f) significantly higher than past (%.0f)".formatted(
                            currentAmount, pastAmount);
                }
            }

            if (pastHadEscalation && "escalate-anomalies".equals(trace.capabilityName())) {
                reason = (reason != null ? reason + "; " : "")
                        + "Past case for similar amount was escalated — threshold may be miscalibrated";
                if (action == AdaptationAction.RETAINED) {
                    action = AdaptationAction.BOOSTED;
                    priority = Math.min(priority + 1, 10);
                }
            }

            if (currentAmount > 0 && pastAmount > 0 && Math.abs(currentAmount / pastAmount - 1.0) >= 0.05) {
                params.put("amountRatio", Math.round(currentAmount / pastAmount * 100) / 100.0);
            }

            steps.add(new AdaptedStep(trace.bindingName(), trace.capabilityName(),
                    trace.workerName(), trace.stepOutcome(), priority,
                    params, action, reason));
        }
        return steps;
    }

    private static double numericFeature(Map<String, FeatureValue> features, String key) {
        FeatureValue fv = features.get(key);
        return fv instanceof FeatureValue.NumberVal nv ? nv.value() : 0.0;
    }
}
