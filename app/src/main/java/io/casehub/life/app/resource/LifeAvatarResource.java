package io.casehub.life.app.resource;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.avatar.AvatarEvent;
import io.casehub.life.app.service.LifeCognitiveAgentService;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.mcp.HandWrittenEndpoint;
import io.smallrye.mutiny.Multi;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

@ApplicationScoped
@Path("/avatar")
@RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
@HandWrittenEndpoint("Avatar conversation — not expressible via @McpDomain")
public class LifeAvatarResource {

    @Inject
    LifeCognitiveAgentService agentService;

    @Inject
    CurrentPrincipal currentPrincipal;

    @POST
    @Blocking
    @Path("/message")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<AvatarEvent> sendMessage(String message) {
        return agentService.sendMessage(currentPrincipal.actorId(), message);
    }

    @DELETE
    @Path("/session")
    public void closeSession() {
        agentService.closeSession(currentPrincipal.actorId());
    }
}
