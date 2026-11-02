package io.casehub.life.app.engine.agent;

import io.casehub.eidos.api.AgentDescriptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LifeAvatarDescriptorProvider {

    private final String name;
    private final String tenancyId;
    private final String jurisdiction;

    @Inject
    public LifeAvatarDescriptorProvider(
            @ConfigProperty(name = "casehub.life.avatar.name", defaultValue = "Life") String name,
            @ConfigProperty(name = "casehub.life.tenancy-id") String tenancyId,
            @ConfigProperty(name = "casehub.life.jurisdiction", defaultValue = "GB") String jurisdiction) {
        this.name = name;
        this.tenancyId = tenancyId;
        this.jurisdiction = jurisdiction;
    }

    public AgentDescriptor getDescriptor() {
        return AgentDescriptor.builder()
                .agentId("life:avatar@1")
                .name(name)
                .version("1")
                .provider("casehub-life")
                .modelFamily("anthropic")
                .slot("avatar")
                .jurisdiction(jurisdiction)
                .tenancyId(tenancyId)
                .briefing(buildBriefing())
                .build();
    }

    private String buildBriefing() {
        return """
            You are the household's digital life assistant. You help manage \
            daily life: tasks, appointments, contractors, finances, and family \
            coordination. You have access to MCP tools for creating tasks, \
            starting cases, looking up people, and checking the calendar.

            Rules:
            - Always confirm before starting a case, creating a financial \
              commitment, or approving an oversight gate
            - Direct lookups, status queries, and low-risk task creation can \
              proceed without confirmation
            - If the user lacks permission for an action, explain what role \
              is needed
            - Be proactive: if you notice overdue items or upcoming deadlines \
              in the context, mention them
            - Be concise but warm""";
    }
}
