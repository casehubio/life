package io.casehub.life.app.coordination.recovery;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TriageRoundTrackerTest {

    private final TriageRoundTracker tracker = new TriageRoundTracker();

    @Test
    void startsAtZero() {
        UUID caseId = UUID.randomUUID();
        assertThat(tracker.roundCount(caseId, "pre-process")).isEqualTo(0);
    }

    @Test
    void incrementsPerWorker() {
        UUID caseId = UUID.randomUUID();
        tracker.increment(caseId, "pre-process");
        assertThat(tracker.roundCount(caseId, "pre-process")).isEqualTo(1);
        tracker.increment(caseId, "pre-process");
        assertThat(tracker.roundCount(caseId, "pre-process")).isEqualTo(2);
    }

    @Test
    void independentPerWorker() {
        UUID caseId = UUID.randomUUID();
        tracker.increment(caseId, "pre-process");
        tracker.increment(caseId, "pre-process");
        assertThat(tracker.roundCount(caseId, "post-process")).isEqualTo(0);
    }

    @Test
    void independentPerCase() {
        UUID case1 = UUID.randomUUID();
        UUID case2 = UUID.randomUUID();
        tracker.increment(case1, "worker-a");
        assertThat(tracker.roundCount(case2, "worker-a")).isEqualTo(0);
    }

    @Test
    void clearRemovesAllWorkersForCase() {
        UUID caseId = UUID.randomUUID();
        tracker.increment(caseId, "worker-a");
        tracker.increment(caseId, "worker-b");
        tracker.clearForCase(caseId);
        assertThat(tracker.roundCount(caseId, "worker-a")).isEqualTo(0);
        assertThat(tracker.roundCount(caseId, "worker-b")).isEqualTo(0);
    }

    @Test
    void isExhaustedAfterTwoRounds() {
        UUID caseId = UUID.randomUUID();
        tracker.increment(caseId, "worker-a");
        assertThat(tracker.isExhausted(caseId, "worker-a")).isFalse();
        tracker.increment(caseId, "worker-a");
        assertThat(tracker.isExhausted(caseId, "worker-a")).isTrue();
    }
}
