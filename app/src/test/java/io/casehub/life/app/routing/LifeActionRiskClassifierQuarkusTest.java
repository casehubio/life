package io.casehub.life.app.routing;

import io.casehub.api.spi.ActionRiskClassifier;
import io.casehub.api.spi.PlannedAction;
import io.casehub.api.spi.RiskClassifier;
import io.casehub.api.spi.RiskDecision;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.casehub.life.api.HouseholdActionType.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LifeActionRiskClassifierQuarkusTest {

    @Inject
    @RiskClassifier
    LifeActionRiskClassifier classifier;

    @Inject
    @RiskClassifier
    Instance<ActionRiskClassifier> riskClassifiers;

    @Test
    void riskClassifierInstance_isSatisfied() {
        assertFalse(riskClassifiers.isUnsatisfied(),
            "@RiskClassifier Instance<ActionRiskClassifier> must not be empty");
    }

    @Test
    void alwaysGateType_returnsGateRequired() {
        PlannedAction action = PlannedAction.of(
            "book specialist", HEALTH_APPOINTMENT_SPECIALIST.actionType(), Map.of());
        assertInstanceOf(RiskDecision.GateRequired.class, classifier.classify(action));
    }

    @Test
    void spendPurchase_belowYamlThreshold_returnsAutonomous() {
        // Confirms risk-policy.yaml loaded: threshold is 100.0
        PlannedAction action = PlannedAction.of(
            "buy groceries", SPEND_PURCHASE.actionType(), Map.of("amount", "99.0"));
        assertInstanceOf(RiskDecision.Autonomous.class, classifier.classify(action));
    }

    @Test
    void spendPurchase_atYamlThreshold_returnsGateRequired() {
        PlannedAction action = PlannedAction.of(
            "buy groceries", SPEND_PURCHASE.actionType(), Map.of("amount", "100.0"));
        assertInstanceOf(RiskDecision.GateRequired.class, classifier.classify(action));
    }

    @Test
    void contractorEngage_atYamlThreshold_returnsGateRequired() {
        PlannedAction action = PlannedAction.of(
            "hire plumber", CONTRACTOR_ENGAGE.actionType(), Map.of("amount", "200.0"));
        assertInstanceOf(RiskDecision.GateRequired.class, classifier.classify(action));
    }
}
