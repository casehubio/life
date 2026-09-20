package io.casehub.life.app.api;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.request.OversightGateRequest;
import io.casehub.life.api.response.BriefingResponse;
import io.casehub.life.api.response.PagedResponse;
import io.casehub.life.api.response.PendingActionResponse;
import io.casehub.life.app.resource.DashboardResource;
import io.casehub.life.app.resource.LifeOversightGateResource;
import io.casehub.life.app.resource.PendingActionsResource;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;

@McpDomain(value = "life/dashboard", basePath = "/api/life/dashboard")
@ApplicationScoped
public class LifeDashboardApi {

    @Inject DashboardResource dashboardResource;
    @Inject PendingActionsResource pendingActionsResource;
    @Inject LifeOversightGateResource oversightResource;

    @PlatformQuery("Get household briefing")
    @RestPath("/briefing")
    public BriefingResponse briefing() {
        return dashboardResource.briefing();
    }

    @PlatformQuery("List pending actions")
    @RestPath("/pending-actions")
    public PagedResponse<PendingActionResponse> pendingActions(
            @QueryParam("domain") LifeDomain domain,
            @QueryParam("candidateGroup") String candidateGroup,
            @QueryParam("dueSoonHours") int dueSoonHours,
            @QueryParam("page") int page,
            @QueryParam("size") int size) {
        return pendingActionsResource.list(domain, candidateGroup, dueSoonHours, page, size);
    }

    @PlatformMutation("Request an oversight gate approval")
    @RestPath("/oversight-gates")
    public Object requestApproval(OversightGateRequest request) {
        return oversightResource.requestApproval(request).getEntity();
    }
}
