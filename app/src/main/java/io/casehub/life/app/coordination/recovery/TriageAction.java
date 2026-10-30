package io.casehub.life.app.coordination.recovery;

public enum TriageAction {
    RETRY_MODIFIED,
    SWITCH_PLAYBOOK,
    GENERATE_FIX,
    FALLBACK_MCP
}
