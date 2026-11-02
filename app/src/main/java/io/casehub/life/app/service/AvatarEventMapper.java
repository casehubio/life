package io.casehub.life.app.service;

import io.casehub.life.api.avatar.AvatarEvent;
import io.casehub.platform.agent.AgentEvent;
import org.jspecify.annotations.Nullable;

public final class AvatarEventMapper {

    private AvatarEventMapper() {}

    public static @Nullable AvatarEvent map(AgentEvent event) {
        return switch (event) {
            case AgentEvent.TextDelta td -> new AvatarEvent.TextDelta(td.text());
            case AgentEvent.ThinkingDelta td -> new AvatarEvent.ThinkingDelta(td.text());
            case AgentEvent.ToolCallDelta ignored -> null;
            case AgentEvent.ToolCallComplete tcc -> new AvatarEvent.ToolCallCompleted(tcc.name(), tcc.arguments());
            case AgentEvent.ToolResult tr -> new AvatarEvent.ToolResult(tr.toolCallId(), tr.content(), tr.isError());
            case AgentEvent.InvocationComplete ic -> new AvatarEvent.SessionComplete(ic.inputTokens(), ic.outputTokens());
        };
    }
}
