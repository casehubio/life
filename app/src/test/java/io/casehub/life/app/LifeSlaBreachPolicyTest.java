package io.casehub.life.app;

import io.casehub.life.app.spi.LifeSlaBreachPolicy;
import io.casehub.work.api.BreachDecision;
import io.casehub.work.api.BreachType;
import io.casehub.work.api.BreachedTask;
import io.casehub.work.api.SlaBreachContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LifeSlaBreachPolicyTest {

    private final LifeSlaBreachPolicy policy = new LifeSlaBreachPolicy();

    private SlaBreachContext ctx(Set<String> candidateGroups) {
        var task = new BreachedTask(UUID.randomUUID(), null, "Test task", candidateGroups);
        // policy does not use scope or preferences — null is safe
        return new SlaBreachContext(BreachType.COMPLETION_EXPIRED, task, null, null);
    }

    @Test
    void firstBreach_escalatesToHouseholdAdmin() {
        var result = policy.onBreach(ctx(Set.of("household-member")));

        assertThat(result).isInstanceOf(BreachDecision.EscalateTo.class);
        var escalate = (BreachDecision.EscalateTo) result;
        assertThat(escalate.groups()).containsExactly("household-admin");
        assertThat(escalate.deadline()).isEqualTo(Duration.ofHours(48));
    }

    @Test
    void secondBreach_adminPresent_failsTerminally() {
        var result = policy.onBreach(ctx(Set.of("household-admin")));

        assertThat(result).isInstanceOf(BreachDecision.Fail.class);
        assertThat(((BreachDecision.Fail) result).reason()).isEqualTo("life-sla-exhausted");
    }

    @Test
    void secondBreach_adminAndOtherGroups_failsTerminally() {
        var result = policy.onBreach(ctx(Set.of("household-admin", "household-member")));

        assertThat(result).isInstanceOf(BreachDecision.Fail.class);
    }

    @Test
    void firstBreach_emptyGroups_escalatesToHouseholdAdmin() {
        var result = policy.onBreach(ctx(Set.of()));

        assertThat(result).isInstanceOf(BreachDecision.EscalateTo.class);
    }
}
