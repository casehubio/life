package io.casehub.life.app.spi;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.response.LifeTaskResponse;
import io.casehub.life.api.spi.LifeTaskVisibilityPolicy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Set;

/**
 * Restricts {@code household-junior} principals to tasks they are directly
 * assigned to or whose candidate groups overlap the principal's groups.
 *
 * <p>Admin and member principals pass through unconditionally.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class JuniorLifeTaskVisibilityPolicy implements LifeTaskVisibilityPolicy {
    @Override
    public boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups) {
        // Non-junior principals are never filtered.
        if (!groups.contains(HouseholdGroups.JUNIOR)) return true;
        if (groups.contains(HouseholdGroups.ADMIN) || groups.contains(HouseholdGroups.MEMBER)) return true;

        // Junior: visible only if assigned or in the candidate pool.
        if (actorId != null && actorId.equals(task.assigneeId())) return true;
        return task.candidateGroups().stream().anyMatch(groups::contains);
    }
}
