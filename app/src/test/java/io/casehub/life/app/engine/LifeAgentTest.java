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
package io.casehub.life.app.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class LifeAgentTest {

    @Test
    void modelFamilyIsOpenclaw() {
        assertThat(LifeAgent.MODEL_FAMILY).isEqualTo("openclaw");
    }

    @Test
    void majorVersionIsOne() {
        assertThat(LifeAgent.MAJOR_VERSION).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void agentIdFollowsConvention(LifeAgent agent) {
        assertThat(agent.agentId())
                .matches("openclaw:[a-z-]+@1")
                .startsWith(LifeAgent.MODEL_FAMILY + ":")
                .endsWith("@" + LifeAgent.MAJOR_VERSION);
    }

    @ParameterizedTest
    @EnumSource(LifeAgent.class)
    void agentIdContainsPersona(LifeAgent agent) {
        assertThat(agent.agentId()).contains(agent.persona());
    }

    @Test
    void healthIdentity() {
        assertThat(LifeAgent.HEALTH.persona()).isEqualTo("health-agent");
        assertThat(LifeAgent.HEALTH.agentId()).isEqualTo("openclaw:health-agent@1");
        assertThat(LifeAgent.HEALTH.displayName()).isEqualTo("OpenClaw Health Agent");
        assertThat(LifeAgent.HEALTH.slot()).isEqualTo("casehubio/life/health");
        assertThat(LifeAgent.HEALTH.briefing()).isEqualTo("Health domain coordination agent");
    }

    @Test
    void homeIdentity() {
        assertThat(LifeAgent.HOME.persona()).isEqualTo("home-agent");
        assertThat(LifeAgent.HOME.agentId()).isEqualTo("openclaw:home-agent@1");
        assertThat(LifeAgent.HOME.displayName()).isEqualTo("OpenClaw Home Agent");
        assertThat(LifeAgent.HOME.slot()).isEqualTo("casehubio/life/household");
        assertThat(LifeAgent.HOME.briefing()).isEqualTo("Household maintenance agent");
    }

    @Test
    void financeIdentity() {
        assertThat(LifeAgent.FINANCE.persona()).isEqualTo("finance-agent");
        assertThat(LifeAgent.FINANCE.agentId()).isEqualTo("openclaw:finance-agent@1");
        assertThat(LifeAgent.FINANCE.displayName()).isEqualTo("OpenClaw Finance Agent");
        assertThat(LifeAgent.FINANCE.slot()).isEqualTo("casehubio/life/finance");
        assertThat(LifeAgent.FINANCE.briefing()).isEqualTo("Financial review and governance agent");
    }

    @Test
    void travelIdentity() {
        assertThat(LifeAgent.TRAVEL.persona()).isEqualTo("travel-agent");
        assertThat(LifeAgent.TRAVEL.agentId()).isEqualTo("openclaw:travel-agent@1");
        assertThat(LifeAgent.TRAVEL.displayName()).isEqualTo("OpenClaw Travel Agent");
        assertThat(LifeAgent.TRAVEL.slot()).isEqualTo("casehubio/life/travel");
        assertThat(LifeAgent.TRAVEL.briefing()).isEqualTo("Travel planning and booking agent");
    }

    @Test
    void fourAgents() {
        assertThat(LifeAgent.values()).hasSize(4);
    }
}
