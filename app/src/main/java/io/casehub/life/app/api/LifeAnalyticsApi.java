package io.casehub.life.app.api;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.response.CaseStatisticsResponse;
import io.casehub.life.api.response.SlaComplianceResponse;
import io.casehub.life.api.response.TrustAnalyticsResponse;
import io.casehub.life.app.service.LifeAnalyticsService;
import java.util.Optional;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;

@McpDomain(value = "life/analytics", app = "life", basePath = "/api/life/analytics", summary = "Life analytics — trends, patterns, outcome tracking")
@ApplicationScoped
public class LifeAnalyticsApi {

    @Inject LifeAnalyticsService resource;

    @PlatformQuery("Get case statistics")
    @RestPath("/cases")
    public CaseStatisticsResponse caseStatistics(@QueryParam("caseType") Optional<String> caseType) {
        return resource.caseStatistics(caseType.orElse(null));
    }

    @PlatformQuery("Get SLA compliance metrics")
    @RestPath("/sla")
    public SlaComplianceResponse slaCompliance(@QueryParam("domain") Optional<LifeDomain> domain) {
        return resource.slaCompliance(domain.orElse(null));
    }

    @PlatformQuery("Get trust analytics")
    @RestPath("/trust")
    public TrustAnalyticsResponse trustAnalytics() {
        return resource.trustAnalytics();
    }
}
