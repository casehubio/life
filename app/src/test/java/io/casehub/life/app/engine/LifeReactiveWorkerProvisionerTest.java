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

import io.casehub.api.model.ProvisionContext;
import io.casehub.api.spi.ProvisionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LifeReactiveWorkerProvisionerTest {

    private LifeReactiveWorkerProvisioner provisioner;
    private LifeSentinelRegistry registry;
    private Scheduler mockScheduler;

    @BeforeEach
    void setup() throws SchedulerException {
        registry = new LifeSentinelRegistry();
        mockScheduler = mock(Scheduler.class);
        when(mockScheduler.scheduleJob(any(), any())).thenReturn(null);

        LifeSentinelConfig config = mock(LifeSentinelConfig.class);
        LifeSentinelConfig.SentinelCapabilityEntry entry = mock(LifeSentinelConfig.SentinelCapabilityEntry.class);
        when(entry.agent()).thenReturn("HOME");
        when(entry.heartbeatInterval()).thenReturn(Duration.ofHours(4));
        when(config.capabilities()).thenReturn(Map.of("contractor-sentinel", entry));

        provisioner = new LifeReactiveWorkerProvisioner();
        provisioner.sentinelRegistry = registry;
        provisioner.sentinelConfig = config;
        provisioner.scheduler = mockScheduler;
    }

    @Test
    void firstProvisionRegistersAndSchedules() throws SchedulerException {
        UUID caseId = UUID.randomUUID();
        ProvisionContext ctx = new ProvisionContext(
                caseId, "test-tenant", "contractor-sentinel",
                null, null, null, null);

        ProvisionResult result = provisioner.provision(Set.of("contractor-sentinel"), ctx)
                .await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(registry.isProvisioned(caseId, "contractor-sentinel")).isTrue();
        verify(mockScheduler).scheduleJob(any(), any());
    }

    @Test
    void secondProvisionIsIdempotent() throws SchedulerException {
        UUID caseId = UUID.randomUUID();
        ProvisionContext ctx = new ProvisionContext(
                caseId, "test-tenant", "contractor-sentinel",
                null, null, null, null);

        provisioner.provision(Set.of("contractor-sentinel"), ctx).await().indefinitely();
        provisioner.provision(Set.of("contractor-sentinel"), ctx).await().indefinitely();

        assertThat(registry.findByCaseId(caseId)).hasSize(1);
        verify(mockScheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void terminateAllForCaseCancelsHeartbeatAndDeregisters() throws SchedulerException {
        UUID caseId = UUID.randomUUID();
        ProvisionContext ctx = new ProvisionContext(
                caseId, "test-tenant", "contractor-sentinel",
                null, null, null, null);

        provisioner.provision(Set.of("contractor-sentinel"), ctx).await().indefinitely();
        provisioner.terminateAllForCase(caseId);

        assertThat(registry.findByCaseId(caseId)).isEmpty();
        verify(mockScheduler).deleteJob(any(JobKey.class));
    }

    @Test
    void getCapabilitiesReturnsConfigKeys() {
        Set<String> capabilities = provisioner.getCapabilities().await().indefinitely();
        assertThat(capabilities).containsExactly("contractor-sentinel");
    }
}
