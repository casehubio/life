package io.casehub.life.app.service;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.response.BriefingItem;
import io.casehub.life.api.response.BriefingResponse;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DashboardService {

    @Inject
    CurrentPrincipal principal;

    @Transactional
    public BriefingResponse buildBriefing() {
        String             greeting  = timeBasedGreeting();
        List<BriefingItem> items     = new ArrayList<>();
        String             tenancyId = principal.tenancyId();

        List<WorkItemEntity> overdue = WorkItemEntity.list(
                "tenancyId = ?1 and status in (?2, ?3) and expiresAt < ?4",
                tenancyId, WorkItemStatus.PENDING, WorkItemStatus.IN_PROGRESS, Instant.now());
        for (WorkItemEntity wi : overdue) {
            LifeDomain domain = resolveDomain(wi);
            items.add(new BriefingItem(wi.title + " — overdue", domain, "sla-breach"));
        }

        Instant endOfDay = Instant.now().plusSeconds(
                LocalTime.of(23, 59).toSecondOfDay() - LocalTime.now().toSecondOfDay());
        List<WorkItemEntity> dueToday = WorkItemEntity.list(
                "tenancyId = ?1 and status in (?2, ?3) and expiresAt >= ?4 and expiresAt <= ?5",
                tenancyId, WorkItemStatus.PENDING, WorkItemStatus.IN_PROGRESS, Instant.now(), endOfDay);
        for (WorkItemEntity wi : dueToday) {
            LifeDomain domain = resolveDomain(wi);
            items.add(new BriefingItem(wi.title + " — due today", domain, "action"));
        }

        return new BriefingResponse(greeting, items.size(), items);}

    private String timeBasedGreeting() {
        int hour = LocalTime.now().getHour();
        String timeOfDay = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";
        return "Good " + timeOfDay;
    }

    private LifeDomain resolveDomain(WorkItemEntity wi) {
        return LifeTaskContext.findByIdOptional(wi.id)
                .map(ctx -> ((LifeTaskContext) ctx).domain)
                .orElse(LifeDomain.HOUSEHOLD);
    }
}
