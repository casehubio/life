package io.casehub.life.app.spi;

import io.casehub.life.api.response.LifeTaskResponse;
import io.casehub.life.api.spi.LifeTaskVisibilityPolicy;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

/**
 * Permissive default — all tasks are visible to all principals.
 * Active only when no {@code @Alternative} visibility policy is selected.
 */
@DefaultBean
@ApplicationScoped
public class DefaultLifeTaskVisibilityPolicy implements LifeTaskVisibilityPolicy {
    @Override
    public boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups) {
        return true;
    }
}
