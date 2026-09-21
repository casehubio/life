package io.casehub.life.app.resource;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.request.CreateHouseholdRequest;
import io.casehub.life.api.request.CreateMemberRequest;
import io.casehub.life.api.response.HouseholdMemberResponse;
import io.casehub.life.api.response.HouseholdResponse;
import io.casehub.life.api.response.OnboardingStatusResponse;
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
@Path("/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OnboardingResource {

    @Inject HouseholdService householdService;

    @GET
    @Path("/status")
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
    public OnboardingStatusResponse status() {
        return householdService.getOnboardingStatus();
    }

    @POST
    @Path("/household")
    @RolesAllowed({HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR})
    public Response createHousehold(CreateHouseholdRequest req) {
        HouseholdResponse response = householdService.createHousehold(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/members")
    @RolesAllowed({HouseholdGroups.ADMIN})
    public Response addMembers(List<CreateMemberRequest> members) {
        List<HouseholdMemberResponse> responses = members.stream()
                .map(householdService::addMember).toList();
        return Response.status(Response.Status.CREATED).entity(responses).build();
    }
}
