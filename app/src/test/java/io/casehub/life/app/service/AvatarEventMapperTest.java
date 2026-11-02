package io.casehub.life.app.service;

import io.casehub.life.api.avatar.AvatarEvent;
import io.casehub.platform.agent.AgentEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvatarEventMapperTest {

    @Test
    void mapsTextDelta() {
        var event = new AgentEvent.TextDelta("hello");
        var result = AvatarEventMapper.map(event);
        assertInstanceOf(AvatarEvent.TextDelta.class, result);
        assertEquals("hello", ((AvatarEvent.TextDelta) result).text());
    }

    @Test
    void mapsThinkingDelta() {
        var event = new AgentEvent.ThinkingDelta("reasoning about...");
        var result = AvatarEventMapper.map(event);
        assertInstanceOf(AvatarEvent.ThinkingDelta.class, result);
    }

    @Test
    void mapsToolCallComplete() {
        var event = new AgentEvent.ToolCallComplete(0, "call-1", "life/tasks/create", "{\"title\":\"Buy milk\"}");
        var result = AvatarEventMapper.map(event);
        assertInstanceOf(AvatarEvent.ToolCallCompleted.class, result);
        assertEquals("life/tasks/create", ((AvatarEvent.ToolCallCompleted) result).toolName());
    }

    @Test
    void mapsToolResult() {
        var event = new AgentEvent.ToolResult("call-1", "{\"id\":1}", false);
        var result = AvatarEventMapper.map(event);
        assertInstanceOf(AvatarEvent.ToolResult.class, result);
        assertFalse(((AvatarEvent.ToolResult) result).isError());
    }

    @Test
    void toolCallDeltaReturnsNull() {
        var event = new AgentEvent.ToolCallDelta(0, "call-1", "life/tasks/create", "{\"ti");
        var result = AvatarEventMapper.map(event);
        assertNull(result);
    }

    @Test
    void mapsInvocationComplete() {
        var event = new AgentEvent.InvocationComplete(1500, 800, 0, 0, 0, 0.05, 2000L, 1800L, "sess-1", 1, false, null, null);
        var result = AvatarEventMapper.map(event);
        assertInstanceOf(AvatarEvent.SessionComplete.class, result);
        assertEquals(1500, ((AvatarEvent.SessionComplete) result).inputTokens());
        assertEquals(800, ((AvatarEvent.SessionComplete) result).outputTokens());
    }
}
