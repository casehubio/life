package io.casehub.life.app.resource;

import io.casehub.life.api.request.CreateLifeTaskRequest;
import io.casehub.life.api.response.LifeTaskResponse;
import io.casehub.life.app.service.LifeTaskService;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Blocking
@ApplicationScoped
@Path("/life-tasks")
public class LifeTaskResource {

    @Inject
    LifeTaskService service;

    @POST
    public Response create(@Valid final CreateLifeTaskRequest req) {
        final LifeTaskResponse created = service.create(req);
        return Response.created(URI.create("/life-tasks/" + created.workItemId()))
                .entity(created)
                .build();
    }
}
