package io.casehub.life.app.cbr;

import io.casehub.neocortex.memory.cbr.AdaptationAction;
import io.casehub.neocortex.memory.cbr.AdaptedPlan;
import io.casehub.neocortex.memory.cbr.AdaptedStep;
import io.casehub.neocortex.memory.cbr.CbrMatch;
import io.casehub.neocortex.memory.cbr.CbrPlanAdapter;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrPlanStep;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.quarkus.arc.All;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Alternative
@Priority(10)
public class LifePlanAdapter implements CbrPlanAdapter {

    private static final Logger LOG = Logger.getLogger(LifePlanAdapter.class);

    private final Map<String, LifeAdaptationRule> rulesByType;
    private final Map<String, LifeAdaptationRule> rulesByCapability;

    @Inject
    public LifePlanAdapter(@All List<LifeAdaptationRule> rules) {
        this.rulesByType = new LinkedHashMap<>();
        this.rulesByCapability = new LinkedHashMap<>();
        for (var rule : rules) {
            var prev = rulesByType.put(rule.caseType(), rule);
            if (prev != null) {
                throw new IllegalStateException(
                        "Duplicate LifeAdaptationRule for caseType '" + rule.caseType()
                        + "': " + prev.getClass().getName() + " and " + rule.getClass().getName());
            }
            for (var cap : rule.knownCapabilities()) {
                var prevCap = rulesByCapability.put(cap, rule);
                if (prevCap != null) {
                    throw new IllegalStateException(
                            "Duplicate capability '" + cap + "': "
                            + prevCap.getClass().getName() + " and " + rule.getClass().getName());
                }
            }
        }
    }

    public AdaptedPlan adapt(CbrMatch<CbrPlanRecord> retrieved,
                             Map<String, FeatureValue> currentFeatures) {
        String inferred = inferCaseType(retrieved.cbrRecord().cbrPlanStep());
        return adapt(inferred, retrieved, currentFeatures);
    }

    @Override
    public AdaptedPlan adapt(String caseType,
                             CbrMatch<CbrPlanRecord> retrieved,
                             Map<String, FeatureValue> currentFeatures) {
        if (retrieved.cbrRecord().cbrPlanStep().isEmpty()) {
            return new AdaptedPlan(List.of());
        }
        LifeAdaptationRule rule = rulesByType.get(caseType);
        if (rule == null) {
            return retainAll(retrieved);
        }
        List<AdaptedStep> steps = rule.adapt(retrieved, currentFeatures);
        return new AdaptedPlan(steps);
    }

    private String inferCaseType(List<CbrPlanStep> traces) {
        for (var trace : traces) {
            LifeAdaptationRule rule = rulesByCapability.get(trace.capabilityName());
            if (rule != null) {
                return rule.caseType();
            }
        }
        return "";
    }

    private AdaptedPlan retainAll(CbrMatch<CbrPlanRecord> retrieved) {
        return new AdaptedPlan(
                retrieved.cbrRecord().cbrPlanStep().stream()
                        .map(t -> new AdaptedStep(t.bindingName(), t.capabilityName(),
                                t.workerName(), t.stepOutcome(), t.priority(),
                                t.parameters(), AdaptationAction.RETAINED, null))
                        .toList());
    }
}
