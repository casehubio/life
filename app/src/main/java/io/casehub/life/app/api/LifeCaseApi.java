package io.casehub.life.app.api;

import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeCaseType;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.request.CreateLifeCaseRequest;
import io.casehub.life.api.response.LifeCaseResponse;
import io.casehub.life.api.response.PagedResponse;
import io.casehub.life.app.resource.LifeCaseResource;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;

import java.util.UUID;

@McpDomain(value = "life/cases", basePath = "/api/life/cases")
@ApplicationScoped
public class LifeCaseApi {

    @Inject LifeCaseResource resource;

    @PlatformMutation("Create a life case")
    @RestPath("/")
    public Object createCase(CreateLifeCaseRequest request) {
        return resource.create(request).getEntity();
    }

    @PlatformQuery("List life cases")
    @RestPath("/")
    public PagedResponse<LifeCaseResponse> listCases(
            @QueryParam("domain") LifeDomain domain,
            @QueryParam("status") LifeCaseStatus status,
            @QueryParam("caseType") LifeCaseType caseType,
            @QueryParam("page") int page,
            @QueryParam("size") int size) {
        return resource.list(domain, status, caseType, page, size);
    }

    @PlatformQuery("Get a life case by ID")
    @RestPath("/{id}")
    public Object getCase(@PathParam UUID id) {
        return resource.findById(id).getEntity();
    }

    @PlatformQuery("List tasks for a case")
    @RestPath("/{id}/tasks")
    public Object listCaseTasks(@PathParam UUID id) {
        return resource.listTasks(id).getEntity();
    }

    @PlatformQuery("List commitments for a case")
    @RestPath("/{id}/commitments")
    public Object listCaseCommitments(@PathParam UUID id) {
        return resource.listCommitments(id).getEntity();
    }

    @PlatformQuery("Get routing decisions for a case")
    @RestPath("/{id}/routing")
    public Object listCaseRouting(@PathParam UUID id) {
        return resource.listRouting(id).getEntity();
    }

    @PlatformQuery("Get CBR precedents for a case")
    @RestPath("/{id}/cbr")
    public Object listCaseCbr(@PathParam UUID id) {
        return resource.listCbrPrecedents(id).getEntity();
    }

    @PlatformQuery("Get channels for a case")
    @RestPath("/{id}/channels")
    public Object listCaseChannels(@PathParam UUID id) {
        return resource.listChannels(id).getEntity();
    }
}
