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
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test alternative to {@link LifeReactiveWorkerProvisioner}.
 * Skips Quartz job scheduling and OpenClaw calls; tracks provision calls for test assertions.
 */
@Alternative
@Priority(10)
@ApplicationScoped
public class TestLifeReactiveWorkerProvisioner extends LifeReactiveWorkerProvisioner {

    @Inject
    LifeSentinelRegistry sentinelRegistry;

    @Inject
    LifeSentinelConfig sentinelConfig;

    private final CopyOnWriteArrayList<ProvisionRecord> provisionCalls = new CopyOnWriteArrayList<>();

    public record ProvisionRecord(UUID caseId, String capabilityName) {}

    @Override
    public Uni<ProvisionResult> provision(Set<String> capabilities, ProvisionContext context) {
        return Uni.createFrom().item(() -> {
            String capabilityName = context.taskType();
            if (sentinelRegistry.isProvisioned(context.caseId(), capabilityName)) {
                return ProvisionResult.empty();
            }
            LifeAgent agent = LifeAgent.valueOf(
                    sentinelConfig.capabilities().get(capabilityName).agent());
            sentinelRegistry.register(new LifeSentinelRegistry.SentinelEntry(
                    agent, context.caseId(), capabilityName, null));
            provisionCalls.add(new ProvisionRecord(context.caseId(), capabilityName));
            return ProvisionResult.empty();
        });
    }

    @Override
    public Uni<Void> terminate(String workerId, String tenancyId) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public void terminateAllForCase(UUID caseId) {
        sentinelRegistry.removeByCaseId(caseId);
    }

    @Override
    public Uni<Set<String>> getCapabilities() {
        return Uni.createFrom().item(() ->
                Set.copyOf(sentinelConfig.capabilities().keySet()));
    }

    public CopyOnWriteArrayList<ProvisionRecord> getProvisionCalls() {
        return provisionCalls;
    }
}
