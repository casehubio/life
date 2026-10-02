package io.casehub.life.app.cbr.adapt;

import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.memory.cbr.AdaptationAction;
import io.casehub.neocortex.memory.cbr.CbrMatch;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrPlanStep;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialAdaptationRuleTest {

    private final FinancialAdaptationRule rule = new FinancialAdaptationRule();

    @Test
    void caseType() {
        assertEquals("financial-review", rule.caseType());
    }

    @Test
    void significantlyHigherAmount_boostsEscalation() {
        var scored = scored(Map.of("amount", FeatureValue.number(1000)));
        Map<String, FeatureValue> current = Map.of("amount", FeatureValue.number(2000));
        var steps = rule.adapt(scored, current);
        var escalateStep = steps.stream()
                .filter(s -> "escalate-anomalies".equals(s.capabilityName())).findFirst().orElseThrow();
        assertEquals(AdaptationAction.BOOSTED, escalateStep.action());
        assertTrue(escalateStep.reason().contains("significantly higher"));
    }

    @Test
    void moderateIncrease_retains() {
        var scored = scored(Map.of("amount", FeatureValue.number(1000)));
        Map<String, FeatureValue> current = Map.of("amount", FeatureValue.number(1200));
        var steps = rule.adapt(scored, current);
        var escalateStep = steps.stream()
                .filter(s -> "escalate-anomalies".equals(s.capabilityName())).findFirst().orElseThrow();
        assertEquals(AdaptationAction.RETAINED, escalateStep.action());
    }

    @Test
    void pastEscalation_flagsMiscalibration() {
        var past = new CbrPlanRecord("p", "s", "COMPLETED", Confidence.unknown(0.9),
                Map.of("amount", FeatureValue.number(1000)),
                List.of(new CbrPlanStep("b1", "escalate-anomalies", "w1", "escalated-to-admin", 5, Map.of(), null)), null, null);
        var scored = new CbrMatch<>(past, "c1", 0.8);
        Map<String, FeatureValue> current = Map.of("amount", FeatureValue.number(1000));
        var steps = rule.adapt(scored, current);
        assertTrue(steps.getFirst().reason().contains("miscalibrated"));
    }

    @Test
    void amountDelta_addsRatio() {
        var scored = scored(Map.of("amount", FeatureValue.number(1000)));
        Map<String, FeatureValue> current = Map.of("amount", FeatureValue.number(1300));
        var steps = rule.adapt(scored, current);
        assertEquals(1.3, steps.getFirst().parameters().get("amountRatio"));
    }

    @Test
    void noFeatures_retainsAll() {
        var scored = scored(Map.of());
        assertTrue(rule.adapt(scored, Map.of()).stream()
                .allMatch(s -> s.action() == AdaptationAction.RETAINED));
    }

    @Test
    void emptyTrace_returnsEmpty() {
        var past = new CbrPlanRecord("p", "s", "COMPLETED", Confidence.unknown(0.9), Map.of(), List.of(), null, null);
        assertTrue(rule.adapt(new CbrMatch<>(past, "c1", 0.8), Map.of()).isEmpty());
    }

    private CbrMatch<CbrPlanRecord> scored(Map<String, FeatureValue> features) {
        return new CbrMatch<>(
                new CbrPlanRecord("problem", "solution", "COMPLETED", Confidence.unknown(0.9), features,
                        List.of(new CbrPlanStep("b1", "gather-data", "w1", "ok", 5, Map.of(), null),
                                new CbrPlanStep("b2", "escalate-anomalies", "w2", "ok", 5, Map.of(), null)), null, null),
                "case-1", 0.85);
    }
}
