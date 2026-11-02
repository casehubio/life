package io.casehub.life.app.service;

import io.casehub.life.api.avatar.AvatarEvent;
import io.casehub.life.app.engine.agent.LifeAvatarDescriptorProvider;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LifeCognitiveAgentService {

    private final AgentProvider agentProvider;
    private final LifeAvatarDescriptorProvider descriptorProvider;
    private final LifeDomainContextProvider contextProvider;
    private final ConcurrentHashMap<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Inject
    public LifeCognitiveAgentService(
            AgentProvider agentProvider,
            LifeAvatarDescriptorProvider descriptorProvider,
            LifeDomainContextProvider contextProvider) {
        this.agentProvider = agentProvider;
        this.descriptorProvider = descriptorProvider;
        this.contextProvider = contextProvider;
    }

    public Multi<AvatarEvent> sendMessage(String principalId, String userMessage) {
        var session = sessions.computeIfAbsent(principalId, this::createSession);
        return session.query(userMessage)
                .map(AvatarEventMapper::map)
                .filter(java.util.Objects::nonNull);
    }

    public void closeSession(String principalId) {
        var session = sessions.remove(principalId);
        if (session != null) {
            session.close(Duration.ofSeconds(5));
        }
    }

    private AgentSession createSession(String principalId) {
        var descriptor = descriptorProvider.getDescriptor();
        var context = contextProvider.buildContextSummary();
        var systemPrompt = descriptor.briefing() + "\n\n" + context;
        var init = AgentSessionInit.of(systemPrompt);
        return agentProvider.openSession(init);
    }
}
