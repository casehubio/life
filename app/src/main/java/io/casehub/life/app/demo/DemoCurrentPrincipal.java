package io.casehub.life.app.demo;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Set;

@ApplicationScoped
@Alternative
@Priority(300)
@IfBuildProfile("demo")
public class DemoCurrentPrincipal implements CurrentPrincipal {

    private static final String TENANCY_ID = "278776f9-e1b0-46fb-9032-8bddebdcf9ce";

    @Override
    public String actorId() {
        return "demo-admin";
    }

    @Override
    public Set<String> groups() {
        return Set.of(HouseholdGroups.ADMIN, HouseholdGroups.MEMBER);
    }

    @Override
    public String tenancyId() {
        return TENANCY_ID;
    }

    @Override
    public boolean isCrossTenantAdmin() {
        return false;
    }
}
