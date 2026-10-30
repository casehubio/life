package io.casehub.life.app.observer;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.life.app.service.LifeCalendarSyncService;
import io.casehub.work.api.WorkItemLifecycleEvent;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class LifeCalendarOutboundObserver {

    private static final Logger LOG = Logger.getLogger(LifeCalendarOutboundObserver.class);

    @Inject
    EntityManager em;

    @Inject
    LifeCalendarSyncService syncService;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onWorkItemEvent(@Observes WorkItemLifecycleEvent event) {
        if (event.status() == WorkItemStatus.COMPLETED || event.status() == WorkItemStatus.CANCELLED) {
            syncService.removeWorkItem(event.workItemId());
            return;
        }

        WorkItemEntity workItem = em.find(WorkItemEntity.class, event.workItemId());
        if (workItem == null || workItem.expiresAt == null) return;

        LifeDomain domain = resolveDomain(event.workItemId(), workItem);
        if (domain == null) {
            LOG.debugf("No domain resolved for workItem=%s — skipping calendar sync", event.workItemId());
            return;
        }

        syncService.syncWorkItem(event.workItemId(), workItem.title, domain, workItem.expiresAt);
    }

    private LifeDomain resolveDomain(java.util.UUID workItemId, WorkItemEntity workItem) {
        LifeDomain domain = LifeDecisionLedgerObserver.domainFromScope(workItem.scope);
        if (domain != null) return domain;
        return Optional.ofNullable(em.find(LifeTaskContext.class, workItemId))
                .map(ctx -> ctx.domain)
                .orElse(null);
    }
}
