package io.casehub.life.app.api;

import io.casehub.life.api.request.CommitmentRequest;
import io.casehub.life.api.request.CreateLifeTaskRequest;
import io.casehub.life.api.response.LifeTaskResponse;
import io.casehub.life.app.resource.LifeCommitmentResource;
import io.casehub.life.app.resource.LifeTaskResource;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@McpDomain(value = "life/tasks", basePath = "/api/life/tasks")
@ApplicationScoped
public class LifeTaskApi {

    @Inject LifeTaskResource taskResource;
    @Inject LifeCommitmentResource commitmentResource;

    @PlatformMutation("Create a life task")
    @RestPath("/")
    public Object createTask(CreateLifeTaskRequest request) {
        return taskResource.create(request).getEntity();
    }

    @PlatformQuery("Get a life task by ID")
    @RestPath("/{id}")
    public LifeTaskResponse getTask(@PathParam UUID id) {
        return taskResource.get(id);
    }

    @PlatformMutation("Apply a commitment to a task")
    @RestPath("/{id}/commit")
    public Object commitToTask(@PathParam UUID id, CommitmentRequest request) {
        return commitmentResource.commit(id, request).getEntity();
    }
}
