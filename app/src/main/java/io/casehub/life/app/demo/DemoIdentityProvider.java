package io.casehub.life.app.demo;

import io.casehub.life.api.HouseholdGroups;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@ApplicationScoped
@Alternative
@Priority(300)
@IfBuildProfile("demo")
public class DemoIdentityProvider implements IdentityProvider<AnonymousAuthenticationRequest> {

    @Override
    public Class<AnonymousAuthenticationRequest> getRequestType() {
        return AnonymousAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(AnonymousAuthenticationRequest request,
                                               AuthenticationRequestContext context) {
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "demo-admin")
                .addRole(HouseholdGroups.ADMIN)
                .addRole(HouseholdGroups.MEMBER)
                .setAnonymous(false)
                .build());
    }
}
