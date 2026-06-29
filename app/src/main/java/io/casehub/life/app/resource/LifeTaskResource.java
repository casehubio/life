package io.casehub.life.app.resource;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.request.CreateLifeTaskRequest;
import io.casehub.life.api.response.LifeTaskResponse;
import io.casehub.life.api.spi.LifeTaskVisibilityPolicy;
import io.casehub.life.app.service.LifeTaskService;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Blocking
@ApplicationScoped
@Path("/life-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LifeTaskResource {

    @Inject
    LifeTaskService service;

    @Inject
    LifeTaskVisibilityPolicy visibilityPolicy;

    @Inject
    CurrentPrincipal principal;

    @POST
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER})
    public Response create(@Valid final CreateLifeTaskRequest req) {
        final LifeTaskResponse created = service.create(req);
        return Response.created(URI.create("/life-tasks/" + created.workItemId()))
                .entity(created)
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
    public LifeTaskResponse get(@PathParam("id") final UUID workItemId) {
        final LifeTaskResponse response = service.get(workItemId);
        if (!visibilityPolicy.isVisible(response, principal.actorId(), principal.groups())) {
            throw new WebApplicationException(404);
        }
        return response;
    }
}
