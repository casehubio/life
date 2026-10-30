package io.casehub.life.app.coordination.recovery;

import java.util.Map;

public record TriageOutcome(
    TriageAction action,
    boolean shouldReschedule,
    Map<String, Object> modifiedParams,
    String appliedTemplateName) {

    public static TriageOutcome reschedule(TriageAction action, Map<String, Object> params, String template) {
        return new TriageOutcome(action, true, params, template);
    }

    public static TriageOutcome fallback() {
        return new TriageOutcome(TriageAction.FALLBACK_MCP, false, Map.of(), null);
    }
}
