package io.casehub.life.app.resource;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.request.CreateMemberRequest;
import io.casehub.life.api.response.HouseholdCapabilitiesResponse;
import io.casehub.life.api.response.HouseholdMemberResponse;
import io.casehub.life.api.response.HouseholdResponse;
import io.casehub.life.app.service.HouseholdService;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Blocking
@ApplicationScoped
@Path("/household")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HouseholdResource {

    @Inject HouseholdService householdService;

    @GET
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
    public HouseholdResponse getHousehold() {
        return householdService.getHousehold();
    }

    @GET @Path("/members")
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER})
    public List<HouseholdMemberResponse> listMembers() {
        return householdService.listMembers();
    }

    @POST @Path("/members")
    @RolesAllowed({HouseholdGroups.ADMIN})
    public Response addMember(CreateMemberRequest req) {
        HouseholdMemberResponse response = householdService.addMember(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET @Path("/capabilities")
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
    public HouseholdCapabilitiesResponse capabilities() {
        return householdService.getCapabilities();
    }
}
