package io.casehub.life.app.spi;

import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.oidc.SecurityIdentityCurrentPrincipal;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class LifeCurrentPrincipalProducer {

    @Inject
    SecurityIdentityCurrentPrincipal oidcPrincipal;

    @Produces
    @Alternative
    @Priority(999)
    public CurrentPrincipal currentPrincipal() {
        return oidcPrincipal;
    }
}
