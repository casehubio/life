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
public class HouseholdProcurementAdaptationRule implements LifeAdaptationRule {

    private static final Set<String> CAPABILITIES = Set.of(
            "analyse-need", "search-products", "fulfil-order",
            "track-delivery", "procurement-sentinel");

    @Override
    public String caseType() {
        return "household-procurement";
    }

    @Override
    public Set<String> knownCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public List<AdaptedStep> adapt(CbrMatch<CbrPlanRecord> retrieved,
                                   Map<String, FeatureValue> currentFeatures) {
        CbrPlanRecord past = retrieved.cbrRecord();
        double currentBudget = numericFeature(currentFeatures, "budget");
        double pastBudget = numericFeature(past.features(), "budget");

        List<AdaptedStep> steps = new ArrayList<>();
        for (CbrPlanStep trace : past.cbrPlanStep()) {
            Map<String, Object> params = new LinkedHashMap<>(trace.parameters());
            int priority = trace.priority();
            AdaptationAction action = AdaptationAction.RETAINED;
            String reason = null;

            if (currentBudget > 0 && pastBudget > 0 && currentBudget > pastBudget * 1.3) {
                if ("search-products".equals(trace.capabilityName())) {
                    priority = Math.min(priority + 1, 10);
                    action = AdaptationAction.BOOSTED;
                    reason = "Higher budget (%.0f vs %.0f) — expand product search range".formatted(
                            currentBudget, pastBudget);
                }
            }

            if (currentBudget > 0 && pastBudget > 0 && Math.abs(currentBudget / pastBudget - 1.0) >= 0.05) {
                params.put("budgetRatio", Math.round(currentBudget / pastBudget * 100) / 100.0);
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
