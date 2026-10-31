package io.casehub.life.app.resource;

import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarConflictStore;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.api.request.ResolveConflictRequest;
import io.casehub.life.api.response.CalendarConflictResponse;
import io.casehub.life.api.response.LifeCalendarEventResponse;
import io.casehub.life.api.response.SyncResultResponse;
import io.casehub.life.app.service.LifeCalendarConflictService;
import io.casehub.life.app.service.LifeCalendarInboundSyncJob;
import io.casehub.life.app.service.LifeCalendarSyncService;
import io.casehub.platform.api.mcp.HandWrittenEndpoint;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @Inject
    LifeCalendarConflictStore conflictStore;

    @Inject
    LifeCalendarConflictService conflictService;

    @Inject
    LifeCalendarInboundSyncJob syncJob;

    @Inject
    LifeCalendarMappingStore mappingStore;

    @GET
    @Path("/events")
    public List<LifeCalendarEventResponse> listEvents(
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        Instant start = from != null ? Instant.parse(from) : Instant.now();
        Instant end = to != null ? Instant.parse(to) : start.plusSeconds(7 * 86400);
        return calendarSyncService.listEvents(start, end);
    }

    @GET
    @Path("/conflicts")
    public List<CalendarConflictResponse> listConflicts() {
        return conflictStore.findUnresolved().stream().map(this::toConflictResponse).toList();
    }

    @GET
    @Path("/conflicts/count")
    public long countConflicts() {
        return conflictStore.countUnresolved();
    }

    @GET
    @Path("/conflicts/{id}")
    public CalendarConflictResponse getConflict(@PathParam("id") UUID id) {
        var conflict = conflictStore.findById(id)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        return toConflictResponse(conflict);
    }

    @POST
    @Path("/conflicts/{id}/resolve")
    public Response resolveConflict(@PathParam("id") UUID id, ResolveConflictRequest request) {
        if ("ACCEPT".equals(request.action())) {
            conflictService.accept(id);
        } else if ("DISMISS".equals(request.action())) {
            conflictService.dismiss(id);
        } else {
            throw new WebApplicationException("Invalid action: must be ACCEPT or DISMISS", Response.Status.BAD_REQUEST);
        }
        return Response.ok().build();
    }

    @POST
    @Path("/sync")
    public SyncResultResponse triggerSync() {
        int conflicts = syncJob.executeSync();
        return new SyncResultResponse(conflicts);
    }

    private CalendarConflictResponse toConflictResponse(LifeCalendarConflict conflict) {
        var mapping = mappingStore.findById(conflict.mappingId()).orElse(null);
        return new CalendarConflictResponse(
                conflict.id(), conflict.mappingId(), conflict.conflictType(),
                conflict.detectedAt(), conflict.externalSummary(), conflict.externalTiming(),
                conflict.status(), conflict.resolvedAt(), conflict.resolvedBy(), conflict.domain(),
                mapping != null ? mapping.entityId() : null,
                mapping != null ? mapping.summary() : null,
                mapping != null ? mapping.pushedTiming() : null);
    }
}
