package io.casehub.life.app.spi;

import io.casehub.work.api.BreachDecision;
import io.casehub.work.api.SlaBreachContext;
import io.casehub.work.api.SlaBreachPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;

@ApplicationScoped
public class LifeSlaBreachPolicy implements SlaBreachPolicy {

    @Override
    public BreachDecision onBreach(final SlaBreachContext ctx) {
        // Tier 2 detected: EscalateTo previously updated candidateGroups to include household-admin.
        // Tier detection is safe because CreateLifeTaskRequest forbids candidateGroups overrides —
        // the only way household-admin appears here is from a prior EscalateTo execution.
        if (ctx.task().candidateGroups().contains("household-admin")) {
            return new BreachDecision.Fail("life-sla-exhausted");
        }
        // Tier 1: first breach — escalate to household-admin with a 48h window.
        // 48h is a Layer 2 constant; production would derive from template's defaultExpiryHours.
        return BreachDecision.EscalateTo.to("household-admin").withDeadline(Duration.ofHours(48));
    }
}
