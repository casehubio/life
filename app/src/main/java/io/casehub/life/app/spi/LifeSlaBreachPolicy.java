package io.casehub.life.app.spi;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.LifeSlaPolicy;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.work.api.BreachDecision;
import io.casehub.work.api.SlaBreachContext;
import io.casehub.work.api.spi.SlaBreachPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@ApplicationScoped
public class LifeSlaBreachPolicy implements SlaBreachPolicy {

    @Inject
    EntityManager em;

    @Override
    public String id() {
        return "life-sla-breach";
    }


    @Override
    public BreachDecision onBreach(final SlaBreachContext ctx) {
        LifeDomain domain = resolveDomain(ctx);
        LifeSlaPolicy policy = domain.descriptor().slaPolicy();

        // Tier detection: if escalation group is already in candidateGroups, tier 2 is exhausted.
        if (ctx.task().candidateGroups().contains(policy.escalationGroup())) {
            return new BreachDecision.Fail("life-sla-exhausted");
        }
        return BreachDecision.EscalateTo.to(policy.escalationGroup())
                .withDeadline(policy.escalationDeadline());
    }

    // Protected for unit-test override — BreachedTask only exposes taskId/callerRef/title/candidateGroups,
    // so the domain must be looked up via LifeTaskContext.
    protected LifeDomain resolveDomain(SlaBreachContext ctx) {
        return Optional.ofNullable(em.find(LifeTaskContext.class, ctx.task().taskId()))
                .map(tc -> tc.domain)
                .orElse(LifeDomain.HOUSEHOLD);
    }
}
