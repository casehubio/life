package io.casehub.life.app.observer;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.LifeDecisionEventType;
import io.casehub.life.app.entity.LifeCommitmentRecord;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.life.app.service.ledger.LifeLedgerWriter;
import io.casehub.work.runtime.event.SlaBreachEvent;
import io.casehub.work.runtime.event.WorkItemLifecycleEvent;
import io.casehub.work.runtime.model.WorkItem;
import io.casehub.work.runtime.model.WorkItemStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LifeDecisionLedgerObserver {

    @Inject LifeLedgerWriter lifeLedgerWriter;

    static LifeDomain domainFromScope(String scope) {
        if (scope == null || scope.isEmpty()) return null;
        String[] segments = scope.split("/");
        if (segments.length < 3) return null;
        try {
            return LifeDomain.valueOf(segments[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LifeDomain resolveDomain(java.util.UUID workItemId, WorkItem workItem) {
        LifeDomain domain = domainFromScope(workItem.scope);
        if (domain != null) return domain;
        var ctx = LifeTaskContext.<LifeTaskContext>findByIdOptional(workItemId).orElse(null);
        return ctx != null ? ctx.domain : null;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onSlaBreachEvent(@Observes final SlaBreachEvent event) {
        final var taskId = event.context().task().taskId();
        final var workItem = WorkItem.<WorkItem>findByIdOptional(taskId).orElse(null);
        if (workItem == null) return;

        final LifeDomain domain = resolveDomain(taskId, workItem);
        if (domain == null) return;

        final var ctx = LifeTaskContext.<LifeTaskContext>findByIdOptional(taskId).orElse(null);

        switch (domain) {
            case HEALTH -> {
                if (ctx != null) lifeLedgerWriter.writeHealthEntry(LifeDecisionEventType.SLA_BREACH, ctx, workItem);
            }
            case LEGAL -> {
                if (ctx != null) lifeLedgerWriter.writeLegalEntry(LifeDecisionEventType.SLA_BREACH, ctx, workItem);
            }
            case FINANCE -> LifeCommitmentRecord.findByWorkItemId(taskId).ifPresent(record ->
                    lifeLedgerWriter.writeFinancialEntry(LifeDecisionEventType.SLA_BREACH, record, taskId));
            default -> { }
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onLifecycleEvent(@Observes final WorkItemLifecycleEvent event) {
        if (event.status() != WorkItemStatus.COMPLETED) return;

        final var taskId = event.workItemId();
        final var workItem = WorkItem.<WorkItem>findByIdOptional(taskId).orElse(null);
        if (workItem == null) return;

        final LifeDomain domain = resolveDomain(taskId, workItem);
        if (domain == null) return;

        final var ctx = LifeTaskContext.<LifeTaskContext>findByIdOptional(taskId).orElse(null);

        switch (domain) {
            case HEALTH -> {
                if (ctx != null) lifeLedgerWriter.writeHealthEntry(LifeDecisionEventType.COMPLETED, ctx, workItem);
            }
            case LEGAL -> {
                if (ctx != null) lifeLedgerWriter.writeLegalEntry(LifeDecisionEventType.COMPLETED, ctx, workItem);
            }
            case FINANCE -> LifeCommitmentRecord.findByWorkItemId(taskId).ifPresent(record ->
                    lifeLedgerWriter.writeFinancialEntry(LifeDecisionEventType.COMPLETED, record, taskId));
            default -> { }
        }
    }
}
