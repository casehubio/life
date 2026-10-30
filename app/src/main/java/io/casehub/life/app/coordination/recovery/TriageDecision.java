package io.casehub.life.app.coordination.recovery;

import java.util.Map;

public record TriageDecision(
    TriageAction action,
    String reasoning,
    Map<String, Object> modifiedParams,
    String alternateTemplateName,
    String fixedPlaybookYaml,
    String fixedPlaybookName,
    String fallbackReason) {}
