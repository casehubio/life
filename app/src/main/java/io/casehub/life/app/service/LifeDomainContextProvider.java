package io.casehub.life.app.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class LifeDomainContextProvider {

    private final DashboardService dashboardService;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    @Inject
    public LifeDomainContextProvider(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public String buildContextSummary() {
        dirty.set(false);

        var briefing = dashboardService.buildBriefing();
        var sb = new StringBuilder();
        sb.append("## Current Life State\n\n");
        sb.append("Active items: ").append(briefing.actionCount()).append("\n\n");

        if (!briefing.items().isEmpty()) {
            sb.append("### Attention Items\n");
            var items = briefing.items();
            int limit = Math.min(items.size(), 10);
            for (int i = 0; i < limit; i++) {
                var item = items.get(i);
                sb.append("- [").append(item.domain()).append("] ").append(item.text()).append("\n");
            }
            if (items.size() > limit) {
                sb.append("- ... and ").append(items.size() - limit).append(" more (use tools to query)\n");
            }
        }

        return sb.toString();
    }

    public void invalidate() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }
}
