package io.casehub.life.app.engine;

import io.casehub.api.spi.ProvisionerConfigRegistry;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;

@Mock
@ApplicationScoped
public class TestProvisionerConfigRegistry implements ProvisionerConfigRegistry {

    @Override
    public Map<String, Object> configFor(String providerName, String agentId) {
        return Map.of();
    }

    @Override
    public Set<String> declaredAgentIds(String providerName) {
        return Set.of();
    }
}
