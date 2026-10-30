package io.casehub.life.app.coordination.recovery;

import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.StepResult;
import java.util.List;
import java.util.Map;

public record TriageRequest(
    String workerName,
    String capabilityName,
    String templateName,
    PlaybookExecutionResult executionResult,
    List<StepResult> steps,
    int triageRound,
    List<String> catalogTemplates,
    Map<String, Object> caseContext) {}
