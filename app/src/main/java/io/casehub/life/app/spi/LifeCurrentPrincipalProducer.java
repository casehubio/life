package io.casehub.life.app.spi;

import io.casehub.api.spi.ProvisionerConfigRegistry;
import io.casehub.ledger.api.spi.LedgerEntryRepository;
import io.casehub.ledger.api.spi.LedgerMerkleFrontierRepository;
import io.casehub.ledger.runtime.repository.jpa.JpaLedgerEntryRepository;
import io.casehub.ledger.runtime.repository.jpa.JpaLedgerMerkleFrontierRepository;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.identity.GroupMember;
import io.casehub.platform.api.identity.GroupMembershipProvider;
import io.casehub.platform.oidc.SecurityIdentityCurrentPrincipal;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class LifeCurrentPrincipalProducer {

    @Inject SecurityIdentityCurrentPrincipal oidcPrincipal;
    @Inject JpaLedgerEntryRepository jpaLedgerEntryRepo;
    @Inject JpaLedgerMerkleFrontierRepository jpaFrontierRepo;

    @Produces @Alternative @Priority(999)
    public CurrentPrincipal currentPrincipal() {
        return oidcPrincipal;
    }

    @Produces @Alternative @Priority(999)
    public LedgerEntryRepository ledgerEntryRepository() {
        return jpaLedgerEntryRepo;
    }

    @Produces @Alternative @Priority(999)
    public LedgerMerkleFrontierRepository ledgerMerkleFrontierRepository() {
        return jpaFrontierRepo;
    }

    @Produces @Alternative @Priority(999)
    public GroupMembershipProvider groupMembershipProvider() {
        return (group, tenancyId) -> Set.of();
    }

    @Produces @Alternative @Priority(999)
    public ProvisionerConfigRegistry provisionerConfigRegistry() {
        return new ProvisionerConfigRegistry() {
            @Override public Map<String, Object> configFor(String providerName, String agentId) { return Map.of(); }
            @Override public Set<String> declaredAgentIds(String providerName) { return Set.of(); }
        };
    }
}
