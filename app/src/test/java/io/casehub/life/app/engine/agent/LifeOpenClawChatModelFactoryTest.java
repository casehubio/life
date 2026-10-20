package io.casehub.life.app.engine.agent;

import io.casehub.life.app.engine.LifeAgent;
import io.casehub.openclaw.casehub.DirectCallBridge;
import io.casehub.openclaw.client.OpenClawClientConfig;
import io.casehub.openclaw.client.OpenClawHookClient;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentSessionConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifeOpenClawChatModelFactoryTest {
    @Test
    void nativeProviderPreservesPersonaDeliveryAndTimeout() {
        var bridge = mock(DirectCallBridge.class);
        var hookClient = mock(OpenClawHookClient.class);
        var config = mock(OpenClawClientConfig.class, RETURNS_DEEP_STUBS);
        when(config.delivery().baseUrl()).thenReturn("https://delivery.example");
        when(config.delivery().token()).thenReturn(Optional.of("delivery-token"));
        when(config.agent().defaultTimeoutSeconds()).thenReturn(37);
        when(bridge.submit(anyString(), any())).thenReturn(CompletableFuture.completedFuture("{}"));
        var provider = new LifeOpenClawChatModelFactory(bridge, hookClient, config)
                .forAgent(LifeAgent.HEALTH);

        var events = provider.invoke(AgentSessionConfig.of("system", "input"))
                .collect().asList().await().atMost(Duration.ofSeconds(1));

        assertThat(events).containsExactly(new AgentEvent.TextDelta("{}"));
        verify(bridge).submit(anyString(), eq(Duration.ofSeconds(37)));
        verify(hookClient).invokeDirect(eq(LifeAgent.HEALTH.persona()), eq("system\n\ninput"),
                isNull(), eq(37), contains("https://delivery.example/openclaw/direct-call/"));

        provider.invoke(AgentSessionConfig.of("system", "input", Duration.ofSeconds(12)))
                .collect().asList().await().atMost(Duration.ofSeconds(1));
        verify(bridge).submit(anyString(), eq(Duration.ofSeconds(12)));
        verify(hookClient).invokeDirect(anyString(), anyString(), isNull(), eq(12),
                endsWith("?token=delivery-token"));
    }
}
