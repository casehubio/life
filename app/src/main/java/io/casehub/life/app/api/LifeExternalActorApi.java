package io.casehub.life.app.api;

import io.casehub.life.api.LifeActorType;
import io.casehub.life.api.request.CreateExternalActorRequest;
import io.casehub.life.api.request.UpdateExternalActorRequest;
import io.casehub.life.api.response.ExternalActorResponse;
import io.casehub.life.api.response.PagedResponse;
import io.casehub.life.app.resource.ExternalActorResource;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;

import java.util.UUID;

@McpDomain(value = "life/actors", basePath = "/api/life/actors")
@ApplicationScoped
public class LifeExternalActorApi {

    @Inject ExternalActorResource resource;

    @PlatformMutation("Create an external actor")
    @RestPath("/")
    public Object createActor(CreateExternalActorRequest request) {
        return resource.create(request).getEntity();
    }

    @PlatformQuery("Search external actors")
    @RestPath("/")
    public PagedResponse<ExternalActorResponse> listActors(
            @QueryParam("actorType") LifeActorType actorType,
            @QueryParam("name") String name,
            @QueryParam("contactMethod") String contactMethod,
            @QueryParam("erasedOnly") boolean erasedOnly,
            @QueryParam("page") int page,
            @QueryParam("size") int size) {
        return resource.list(actorType, name, contactMethod, erasedOnly, page, size);
    }

    @PlatformQuery("Get an external actor by ID")
    @RestPath("/{id}")
    public Object getActor(@PathParam UUID id) {
        return resource.get(id).getEntity();
    }

    @PlatformMutation("Update an external actor")
    @RestPath("/{id}")
    public Object updateActor(@PathParam UUID id, UpdateExternalActorRequest request) {
        return resource.update(id, request).getEntity();
    }

    @PlatformMutation("Delete an external actor")
    @RestPath("/{id}/delete")
    public Object deleteActor(@PathParam UUID id) {
        return resource.delete(id).getEntity();
    }

    @PlatformMutation("Erase personal data (GDPR)")
    @RestPath("/{id}/personal-data/erase")
    public Object erasePersonalData(@PathParam UUID id) {
        return resource.erasePersonalData(id).getEntity();
    }

    @PlatformQuery("Get tasks for an external actor")
    @RestPath("/{id}/tasks")
    public Object listActorTasks(@PathParam UUID id) {
        return resource.listTasks(id).getEntity();
    }

    @PlatformQuery("Get trust history for an external actor")
    @RestPath("/{id}/trust-history")
    public Object trustHistory(@PathParam UUID id,
                                @QueryParam("page") int page,
                                @QueryParam("size") int size) {
        return resource.trustHistory(id, page, size).getEntity();
    }

    @PlatformQuery("Get activity timeline for an external actor")
    @RestPath("/{id}/activity")
    public Object activityTimeline(@PathParam UUID id,
                                    @QueryParam("page") int page,
                                    @QueryParam("size") int size) {
        return resource.activityTimeline(id, page, size).getEntity();
    }
}
