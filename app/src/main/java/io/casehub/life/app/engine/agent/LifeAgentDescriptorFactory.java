/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.life.app.engine.agent;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.life.app.engine.LifeAgent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LifeAgentDescriptorFactory {

    private final String tenancyId;
    private final String jurisdiction;

    @Inject
    public LifeAgentDescriptorFactory(
            @ConfigProperty(name = "casehub.life.tenancy-id") String tenancyId,
            @ConfigProperty(name = "casehub.life.jurisdiction", defaultValue = "GB") String jurisdiction) {
        this.tenancyId = tenancyId;
        this.jurisdiction = jurisdiction;
    }

    public AgentDescriptor descriptorFor(LifeAgent agent) {
        return AgentDescriptor.builder()
                .agentId(agent.agentId())
                .name(agent.displayName())
                .version(String.valueOf(LifeAgent.MAJOR_VERSION))
                .provider(LifeAgent.MODEL_FAMILY)
                .modelFamily(LifeAgent.MODEL_FAMILY)
                .slot(agent.slot())
                .jurisdiction(jurisdiction)
                .tenancyId(tenancyId)
                .briefing(agent.briefing())
                .build();
    }
}
