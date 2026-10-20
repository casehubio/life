package io.casehub.life.app.engine.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentSessionConfig;
import io.smallrye.mutiny.Multi;
import io.casehub.life.app.engine.LifeAgent;
import io.casehub.openclaw.casehub.DirectCallBridge;
import io.casehub.openclaw.casehub.OpenClawAgentProvider;
import io.casehub.openclaw.client.OpenClawClientConfig;
import io.casehub.openclaw.client.OpenClawHookClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.function.UnaryOperator;

/** Creates native OpenClaw providers and structured JSON worker invocations. */
@ApplicationScoped
public class LifeOpenClawChatModelFactory {

    private DirectCallBridge bridge;
    private OpenClawHookClient hookClient;
    private String deliveryBaseUrl;
    private String deliveryToken;
    private int timeoutSeconds;

    @Inject
    public LifeOpenClawChatModelFactory(DirectCallBridge bridge,
                                         OpenClawHookClient hookClient,
                                         OpenClawClientConfig config) {
        this.bridge = bridge;
        this.hookClient = hookClient;
        this.deliveryBaseUrl = config.delivery().baseUrl();
        this.deliveryToken = config.delivery().token().orElse(null);
        this.timeoutSeconds = config.agent().defaultTimeoutSeconds();
    }

    /** No-arg constructor for test subclasses that override forAgent() entirely. */
    protected LifeOpenClawChatModelFactory() {
        this.bridge = null;
        this.hookClient = null;
        this.deliveryBaseUrl = null;
        this.deliveryToken = null;
        this.timeoutSeconds = 120;
    }

    public AgentProvider forAgent(LifeAgent agent) {
        return new OpenClawAgentProvider(
                bridge, hookClient, agent.persona(), deliveryBaseUrl, deliveryToken) {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return super.invoke(new AgentSessionConfig(
                        config.systemPrompt(), config.userPrompt(), config.mcpServers(),
                        config.timeout() != null ? config.timeout() : Duration.ofSeconds(timeoutSeconds),
                        config.correlationId(), config.model(), config.modelQuery(), config.modelChain()));
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
                Duration.ofSeconds(timeoutSeconds));
    }
}
