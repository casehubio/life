package io.casehub.life.app.spi;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.response.LifeTaskResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JuniorLifeTaskVisibilityPolicyTest {

    private final JuniorLifeTaskVisibilityPolicy policy = new JuniorLifeTaskVisibilityPolicy();

    @Test
    void adminSeesEverything() {
        var task = taskWithAssignee("other-actor");
        assertTrue(policy.isVisible(task, "admin-actor",
            Set.of(HouseholdGroups.ADMIN)));
    }

    @Test
    void memberSeesEverything() {
        var task = taskWithAssignee("other-actor");
        assertTrue(policy.isVisible(task, "member-actor",
            Set.of(HouseholdGroups.MEMBER)));
    }

    @Test
    void juniorSeesAssignedTask() {
        var task = taskWithAssignee("junior-actor");
        assertTrue(policy.isVisible(task, "junior-actor",
            Set.of(HouseholdGroups.JUNIOR)));
    }

    @Test
    void juniorSeesTaskInCandidatePool() {
        var task = taskWithCandidateGroups(List.of(HouseholdGroups.JUNIOR));
        assertTrue(policy.isVisible(task, "junior-actor",
            Set.of(HouseholdGroups.JUNIOR)));
    }

    @Test
    void juniorCannotSeeOtherTask() {
        var task = taskWithAssignee("admin-actor");
        assertFalse(policy.isVisible(task, "junior-actor",
            Set.of(HouseholdGroups.JUNIOR)));
    }

    @Test
    void juniorCannotSeeTaskWithoutMatchingCandidateGroup() {
        var task = taskWithCandidateGroups(List.of(HouseholdGroups.ADMIN));
        assertFalse(policy.isVisible(task, "junior-actor",
            Set.of(HouseholdGroups.JUNIOR)));
    }

    @Test
    void principalWithBothJuniorAndMemberSeesEverything() {
        var task = taskWithAssignee("other-actor");
        assertTrue(policy.isVisible(task, "hybrid-actor",
            Set.of(HouseholdGroups.JUNIOR, HouseholdGroups.MEMBER)));
    }

    private LifeTaskResponse taskWithAssignee(String assigneeId) {
        return new LifeTaskResponse(UUID.randomUUID(), "test-template",
            LifeDomain.HOUSEHOLD, "READY", null, Instant.now(),
            null, null, assigneeId, List.of());
    }

    private LifeTaskResponse taskWithCandidateGroups(List<String> groups) {
        return new LifeTaskResponse(UUID.randomUUID(), "test-template",
            LifeDomain.HOUSEHOLD, "READY", null, Instant.now(),
            null, null, null, groups);
    }
}
