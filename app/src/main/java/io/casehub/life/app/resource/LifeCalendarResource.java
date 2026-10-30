package io.casehub.life.app.resource;

import io.casehub.life.api.response.LifeCalendarEventResponse;
import io.casehub.life.app.service.LifeCalendarSyncService;
import io.casehub.platform.api.mcp.HandWrittenEndpoint;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

@Blocking
@ApplicationScoped
@Path("/life-calendar")
@HandWrittenEndpoint("calendar sync — dashboard events and mappings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("household-member")
public class LifeCalendarResource {

    @Inject
    LifeCalendarSyncService calendarSyncService;

    @GET
    @Path("/events")
    public List<LifeCalendarEventResponse> listEvents(
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        Instant start = from != null ? Instant.parse(from) : Instant.now();
        Instant end = to != null ? Instant.parse(to) : start.plusSeconds(7 * 86400);
        return calendarSyncService.listEvents(start, end);
    }
}
