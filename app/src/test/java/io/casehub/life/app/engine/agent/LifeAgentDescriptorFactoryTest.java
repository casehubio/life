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

import io.casehub.life.app.engine.LifeAgent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class LifeAgentDescriptorFactoryTest {

    private final LifeAgentDescriptorFactory factory =
            new LifeAgentDescriptorFactory("test-tenant-id", "GB");

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void descriptorMatchesAgentIdentity(LifeAgent agent) {
        var descriptor = factory.descriptorFor(agent);

        assertThat(descriptor.agentId()).isEqualTo(agent.agentId());
        assertThat(descriptor.name()).isEqualTo(agent.displayName());
        assertThat(descriptor.slot()).isEqualTo(agent.slot());
        assertThat(descriptor.briefing()).isEqualTo(agent.briefing());
    }

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void descriptorUsesModelFamilyConstants(LifeAgent agent) {
        var descriptor = factory.descriptorFor(agent);

        assertThat(descriptor.version()).isEqualTo(String.valueOf(LifeAgent.MAJOR_VERSION));
        assertThat(descriptor.provider()).isEqualTo(LifeAgent.MODEL_FAMILY);
        assertThat(descriptor.modelFamily()).isEqualTo(LifeAgent.MODEL_FAMILY);
    }

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void descriptorUsesInjectedConfig(LifeAgent agent) {
        var descriptor = factory.descriptorFor(agent);

        assertThat(descriptor.tenancyId()).isEqualTo("test-tenant-id");
        assertThat(descriptor.jurisdiction()).isEqualTo("GB");
    }

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void descriptorLeavesOptionalFieldsNull(LifeAgent agent) {
        var descriptor = factory.descriptorFor(agent);

        assertThat(descriptor.modelVersion()).isNull();
        assertThat(descriptor.weightsFingerprint()).isNull();
        assertThat(descriptor.domainVocabulary()).isNull();
        assertThat(descriptor.slotVocabulary()).isNull();
        assertThat(descriptor.dispositionVocabulary()).isNull();
        assertThat(descriptor.axisVocabularies()).isNull();
        assertThat(descriptor.capabilities()).isEmpty();
        assertThat(descriptor.disposition()).isNull();
        assertThat(descriptor.dataHandlingPolicy()).isNull();
    }
}
