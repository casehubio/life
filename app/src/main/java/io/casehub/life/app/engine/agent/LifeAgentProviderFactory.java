package io.casehub.life.app.engine.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.life.app.engine.LifeAgent;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class LifeAgentProviderFactory {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    protected LifeAgentProviderFactory() {
    }

    public AgentProvider forAgent(LifeAgent agent) {
        return new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig request) {
                return Multi.createFrom().item(new AgentEvent.TextDelta("{\"ok\":true}"));
            }

            @Override
            public AgentSession openSession(AgentSessionInit init) {
                throw new UnsupportedOperationException("No agent backend configured — wire platform RoutingChatModel");
            }
        };
    }

    public WorkerAgentInvoker invokerFor(LifeAgent agent, String systemPrompt,
                                         String userMessage, Class<?> responseSchema,
                                         UnaryOperator<JsonNode> inputTransformer) {
        var schema = JsonSchemaElementUtils.toMap(
                JsonSchemaElementUtils.jsonSchemaElementFrom(responseSchema));
        String prompt = systemPrompt + "\n\nRespond with JSON matching this schema:\n"
                + new ObjectMapper().valueToTree(schema);
        return new WorkerAgentInvoker(forAgent(agent), prompt, userMessage,
                inputTransformer, UnaryOperator.identity(), null, null,
                Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }
}
