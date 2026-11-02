package io.casehub.life.app.service;

import io.casehub.life.api.avatar.AvatarEvent;
import io.casehub.life.app.engine.agent.LifeAvatarDescriptorProvider;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LifeCognitiveAgentServiceTest {

    private AgentProvider agentProvider;
    private AgentSession agentSession;
    private LifeAvatarDescriptorProvider descriptorProvider;
    private LifeDomainContextProvider contextProvider;
    private LifeCognitiveAgentService service;

    @BeforeEach
    void setUp() {
        agentProvider = mock(AgentProvider.class);
        agentSession = mock(AgentSession.class);
        descriptorProvider = new LifeAvatarDescriptorProvider(
            "Life", "278776f9-e1b0-46fb-9032-8bddebdcf9ce", "GB");
        contextProvider = mock(LifeDomainContextProvider.class);

        when(agentProvider.openSession(any(AgentSessionInit.class))).thenReturn(agentSession);
        when(contextProvider.buildContextSummary()).thenReturn("## Current Life State\nActive items: 2");
        when(contextProvider.isDirty()).thenReturn(false);

        service = new LifeCognitiveAgentService(agentProvider, descriptorProvider, contextProvider);
    }

    @Test
    void sendMessageStreamsAvatarEvents() {
        when(agentSession.query(anyString())).thenReturn(
            Multi.createFrom().items(
                new AgentEvent.TextDelta("Hello! "),
                new AgentEvent.TextDelta("How can I help?")
            )
        );

        List<AvatarEvent> events = service.sendMessage("user-1", "Hi there")
            .collect().asList()
            .await().atMost(Duration.ofSeconds(5));

        assertEquals(2, events.size());
        assertInstanceOf(AvatarEvent.TextDelta.class, events.get(0));
        assertEquals("Hello! ", ((AvatarEvent.TextDelta) events.get(0)).text());
    }

    @Test
    void sendMessageCreatesSessionOnFirstCall() {
        when(agentSession.query(anyString())).thenReturn(
            Multi.createFrom().items(new AgentEvent.TextDelta("Hi")));

        service.sendMessage("user-1", "Hello").collect().asList()
            .await().atMost(Duration.ofSeconds(5));

        verify(agentProvider).openSession(any(AgentSessionInit.class));
    }

    @Test
    void sendMessageReusesExistingSession() {
        when(agentSession.query(anyString())).thenReturn(
            Multi.createFrom().items(new AgentEvent.TextDelta("Hi")));

        service.sendMessage("user-1", "Hello").collect().asList()
            .await().atMost(Duration.ofSeconds(5));
        service.sendMessage("user-1", "What's next?").collect().asList()
            .await().atMost(Duration.ofSeconds(5));

        verify(agentProvider, times(1)).openSession(any(AgentSessionInit.class));
    }

    @Test
    void closeSessionRemovesSession() {
        when(agentSession.query(anyString())).thenReturn(
            Multi.createFrom().items(new AgentEvent.TextDelta("Hi")));

        service.sendMessage("user-1", "Hello").collect().asList()
            .await().atMost(Duration.ofSeconds(5));
        service.closeSession("user-1");

        verify(agentSession).close(any(Duration.class));
    }

    @Test
    void closeNonExistentSessionIsNoOp() {
        service.closeSession("unknown-user");
    }

    @Test
    void differentUsersGetDifferentSessions() {
        when(agentSession.query(anyString())).thenReturn(
            Multi.createFrom().items(new AgentEvent.TextDelta("Hi")));

        service.sendMessage("user-1", "Hello").collect().asList()
            .await().atMost(Duration.ofSeconds(5));
        service.sendMessage("user-2", "Hello").collect().asList()
            .await().atMost(Duration.ofSeconds(5));

        verify(agentProvider, times(2)).openSession(any(AgentSessionInit.class));
    }
}
