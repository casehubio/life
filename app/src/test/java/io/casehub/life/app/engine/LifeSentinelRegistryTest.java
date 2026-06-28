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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LifeSentinelRegistryTest {

    private LifeSentinelRegistry registry;

    @BeforeEach
    void setup() {
        registry = new LifeSentinelRegistry();
    }

    @Test
    void notProvisionedByDefault() {
        assertThat(registry.isProvisioned(UUID.randomUUID(), "contractor-sentinel")).isFalse();
    }

    @Test
    void provisionedAfterRegister() {
        UUID caseId = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, caseId, "contractor-sentinel", new JobKey("test", "sentinels")));
        assertThat(registry.isProvisioned(caseId, "contractor-sentinel")).isTrue();
    }

    @Test
    void notProvisionedForDifferentCapability() {
        UUID caseId = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, caseId, "contractor-sentinel", new JobKey("test", "sentinels")));
        assertThat(registry.isProvisioned(caseId, "maintenance-sentinel")).isFalse();
    }

    @Test
    void notProvisionedForDifferentCase() {
        UUID case1 = UUID.randomUUID();
        UUID case2 = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, case1, "contractor-sentinel", new JobKey("test", "sentinels")));
        assertThat(registry.isProvisioned(case2, "contractor-sentinel")).isFalse();
    }

    @Test
    void concurrentCasesSameAgentType() {
        UUID case1 = UUID.randomUUID();
        UUID case2 = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HEALTH, case1, "follow-up-sentinel", new JobKey("j1", "sentinels")));
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HEALTH, case2, "follow-up-sentinel", new JobKey("j2", "sentinels")));
        assertThat(registry.isProvisioned(case1, "follow-up-sentinel")).isTrue();
        assertThat(registry.isProvisioned(case2, "follow-up-sentinel")).isTrue();
    }

    @Test
    void findByCaseIdReturnsAllEntries() {
        UUID caseId = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, caseId, "contractor-sentinel", new JobKey("j1", "sentinels")));
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.FINANCE, caseId, "anomaly-sentinel", new JobKey("j2", "sentinels")));
        assertThat(registry.findByCaseId(caseId)).hasSize(2);
    }

    @Test
    void findByCaseIdReturnsEmptyForUnknownCase() {
        assertThat(registry.findByCaseId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void removeByCaseIdClearsAll() {
        UUID caseId = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, caseId, "contractor-sentinel", new JobKey("j1", "sentinels")));
        registry.removeByCaseId(caseId);
        assertThat(registry.isProvisioned(caseId, "contractor-sentinel")).isFalse();
        assertThat(registry.findByCaseId(caseId)).isEmpty();
    }

    @Test
    void removeByCaseIdDoesNotAffectOtherCases() {
        UUID case1 = UUID.randomUUID();
        UUID case2 = UUID.randomUUID();
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, case1, "contractor-sentinel", new JobKey("j1", "sentinels")));
        registry.register(new LifeSentinelRegistry.SentinelEntry(
                LifeAgent.HOME, case2, "contractor-sentinel", new JobKey("j2", "sentinels")));
        registry.removeByCaseId(case1);
        assertThat(registry.isProvisioned(case1, "contractor-sentinel")).isFalse();
        assertThat(registry.isProvisioned(case2, "contractor-sentinel")).isTrue();
    }
}
