package io.casehub.life.api.avatar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvatarEventTest {

    @Test
    void textDeltaCarriesText() {
        var event = new AvatarEvent.TextDelta("hello");
        assertEquals("hello", event.text());
        assertInstanceOf(AvatarEvent.class, event);
    }

    @Test
    void thinkingDeltaCarriesText() {
        var event = new AvatarEvent.ThinkingDelta("reasoning");
        assertEquals("reasoning", event.text());
    }

    @Test
    void toolCallStartedCarriesName() {
        var event = new AvatarEvent.ToolCallStarted("life/tasks/create");
        assertEquals("life/tasks/create", event.toolName());
    }

    @Test
    void toolCallCompletedCarriesArguments() {
        var event = new AvatarEvent.ToolCallCompleted("life/tasks/create", "{\"title\":\"Buy milk\"}");
        assertEquals("life/tasks/create", event.toolName());
        assertEquals("{\"title\":\"Buy milk\"}", event.arguments());
    }

    @Test
    void toolResultCarriesErrorFlag() {
        var event = new AvatarEvent.ToolResult("life/tasks/create", "{\"id\":1}", false);
        assertFalse(event.isError());
        var error = new AvatarEvent.ToolResult("life/tasks/create", "not found", true);
        assertTrue(error.isError());
    }

    @Test
    void actionCardCarriesTypeAndData() {
        var event = new AvatarEvent.ActionCard("task-created", "{\"id\":1,\"title\":\"Buy milk\"}");
        assertEquals("task-created", event.type());
    }

    @Test
    void confirmationRequiredCarriesDetails() {
        var event = new AvatarEvent.ConfirmationRequired("action-1", "Start care-coordination case", "Will create WorkItems with SLA deadlines");
        assertEquals("action-1", event.actionId());
        assertEquals("Start care-coordination case", event.description());
    }

    @Test
    void sessionCompleteCarriesTokenUsage() {
        var event = new AvatarEvent.SessionComplete(1500, 800);
        assertEquals(1500, event.inputTokens());
        assertEquals(800, event.outputTokens());
    }

    @Test
    void sealedInterfacePermitsAllVariants() {
        Class<?>[] permitted = AvatarEvent.class.getPermittedSubclasses();
        assertEquals(8, permitted.length);
    }
}
