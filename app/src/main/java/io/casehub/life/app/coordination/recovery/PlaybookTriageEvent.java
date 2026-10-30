package io.casehub.life.app.coordination.recovery;

import io.casehub.api.model.PlaybookExecutionResult;
import java.util.Map;
import java.util.UUID;

public record PlaybookTriageEvent(
    UUID caseId,
    String tenancyId,
    String workerName,
    String capabilityName,
    String templateName,
    TriageAction triageAction,
    String reasoning,
    int triageRound,
    PlaybookExecutionResult executionResult,
    Map<String, Object> caseContext) {}
