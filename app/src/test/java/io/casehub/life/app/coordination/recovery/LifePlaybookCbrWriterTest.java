package io.casehub.life.app.coordination.recovery;

import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class LifePlaybookCbrWriterTest {

    private CbrRecordStore cbrStore;
    private LifePlaybookCbrWriter writer;

    @BeforeEach
    void setUp() {
        cbrStore = Mockito.mock(CbrRecordStore.class);
        writer = new LifePlaybookCbrWriter(cbrStore);
    }

    @Test
    void writesRecordWithPlaybookFeatures() {
        var steps = List.of(
            new StepResult("s1", "fetch-contacts", StepStatus.SUCCEEDED, Duration.ofMillis(200), null),
            new StepResult("s2", "send-email", StepStatus.FAILED, Duration.ofMillis(5000), "timeout"),
            new StepResult("s3", "log-result", StepStatus.SKIPPED, Duration.ZERO, null));

        var execResult = new PlaybookExecutionResult.PartialSuccess(Map.of("partial", true), steps);

        var event = new PlaybookTriageEvent(
            UUID.randomUUID(), "tenant-1", "pre-process",
            "contractor-coordination", "contractor-onboarding",
            TriageAction.RETRY_MODIFIED, "increase timeout",
            1, execResult, Map.of("domain", "CONTRACTOR_COORDINATION"));

        writer.onTriageEvent(event);

        Mockito.verify(cbrStore).store(
            Mockito.any(CbrPlanRecord.class),
            Mockito.eq("playbook-triage"),
            Mockito.eq("pre-process"),
            Mockito.any(),
            Mockito.eq("life-personal"),
            Mockito.anyString(),
            Mockito.any());
    }

    @Test
    void writesRecordForFallbackMcp() {
        var event = new PlaybookTriageEvent(
            UUID.randomUUID(), "tenant-1", "planning-worker",
            null, null,
            TriageAction.FALLBACK_MCP, "planning worker exception",
            0, null, Map.of());

        writer.onTriageEvent(event);

        var captor = ArgumentCaptor.forClass(CbrPlanRecord.class);
        Mockito.verify(cbrStore).store(
            captor.capture(),
            Mockito.eq("playbook-triage"),
            Mockito.eq("planning-worker"),
            Mockito.any(),
            Mockito.eq("life-personal"),
            Mockito.anyString(),
            Mockito.any());

        CbrPlanRecord record = captor.getValue();
        assertThat(record.outcome()).isEqualTo("unresolved");
    }

    @Test
    void categorizesErrors() {
        assertThat(LifePlaybookCbrWriter.categorizeError("Connection timeout after 30s"))
            .isEqualTo("timeout");
        assertThat(LifePlaybookCbrWriter.categorizeError("HTTP 401 Unauthorized"))
            .isEqualTo("auth");
        assertThat(LifePlaybookCbrWriter.categorizeError("HTTP 404 Not Found"))
            .isEqualTo("not-found");
        assertThat(LifePlaybookCbrWriter.categorizeError("HTTP 500 Internal Server Error"))
            .isEqualTo("server-error");
        assertThat(LifePlaybookCbrWriter.categorizeError("some other error"))
            .isEqualTo("other");
        assertThat(LifePlaybookCbrWriter.categorizeError(null))
            .isEqualTo("unknown");
    }
}
