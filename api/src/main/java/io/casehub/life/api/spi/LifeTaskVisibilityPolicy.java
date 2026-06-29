package io.casehub.life.api.spi;

import io.casehub.life.api.response.LifeTaskResponse;

import java.util.Set;

/**
 * Determines whether a given principal may see a particular life task.
 *
 * <p>Default implementation is permissive (always visible). The
 * {@code JuniorLifeTaskVisibilityPolicy} alternative restricts
 * {@code household-junior} principals to tasks they are assigned to
 * or whose candidate groups overlap their own groups.
 */
public interface LifeTaskVisibilityPolicy {
    boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups);
}
