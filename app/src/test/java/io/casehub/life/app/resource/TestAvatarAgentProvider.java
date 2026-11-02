package io.casehub.life.app.resource;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Duration;

@Alternative
@Priority(10)
@ApplicationScoped
public class TestAvatarAgentProvider implements AgentProvider {

    @Override
    public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        return Multi.createFrom().items(new AgentEvent.TextDelta("Test response"));
    }

    @Override
    public AgentSession openSession(AgentSessionInit init) {
        return new AgentSession() {
            @Override
            public Multi<AgentEvent> query(String prompt) {
                return Multi.createFrom().items(new AgentEvent.TextDelta("Hello from avatar!"));
            }

            @Override
            public void close(Duration drain) {}

            @Override
            public Uni<Void> interrupt() {
                return Uni.createFrom().voidItem();
            }
        };
    }
}
