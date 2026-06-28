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
import io.casehub.api.spi.ReactiveWorkerProvisioner;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import io.quarkus.logging.Log;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class LifeReactiveWorkerProvisioner implements ReactiveWorkerProvisioner {

    @Inject
    LifeSentinelRegistry sentinelRegistry;

    @Inject
    LifeSentinelConfig sentinelConfig;

    @Inject
    Scheduler scheduler;

    /**
     * Protected no-arg constructor for TestLifeReactiveWorkerProvisioner subclass.
     */
    protected LifeReactiveWorkerProvisioner() {
    }

    @Override
    public Uni<ProvisionResult> provision(Set<String> capabilities, ProvisionContext context) {
        return Uni.createFrom().item(() -> {
            String capabilityName = context.taskType();

            if (sentinelRegistry.isProvisioned(context.caseId(), capabilityName)) {
                return ProvisionResult.empty();
            }

            LifeAgent agent = resolveAgent(capabilityName);
            Duration interval = sentinelConfig.capabilities()
                    .get(capabilityName).heartbeatInterval();
            JobKey jobKey = scheduleHeartbeat(agent, context.caseId(), capabilityName, interval);

            sentinelRegistry.register(new LifeSentinelRegistry.SentinelEntry(
                    agent, context.caseId(), capabilityName, jobKey));

            Log.infof("Provisioned sentinel: capability=%s agent=%s caseId=%s interval=%s",
                    capabilityName, agent.agentId(), context.caseId(), interval);

            return ProvisionResult.empty();
        });
    }

    @Override
    public Uni<Void> terminate(String workerId, String tenancyId) {
        return Uni.createFrom().voidItem();
    }

    public void terminateAllForCase(UUID caseId) {
        sentinelRegistry.findByCaseId(caseId).forEach(entry -> {
            cancelHeartbeat(entry.heartbeatJobKey());
            Log.infof("Terminated sentinel: capability=%s caseId=%s",
                    entry.capabilityName(), caseId);
        });
        sentinelRegistry.removeByCaseId(caseId);
    }

    @Override
    public Uni<Set<String>> getCapabilities() {
        return Uni.createFrom().item(() ->
                Set.copyOf(sentinelConfig.capabilities().keySet()));
    }

    LifeAgent resolveAgent(String capabilityName) {
        var entry = sentinelConfig.capabilities().get(capabilityName);
        if (entry == null) {
            throw new io.casehub.api.spi.ProvisioningException(
                    "No sentinel configuration for capability: " + capabilityName);
        }
        return LifeAgent.valueOf(entry.agent());
    }

    private JobKey scheduleHeartbeat(LifeAgent agent, UUID caseId,
                                     String capabilityName, Duration interval) {
        String jobName = capabilityName + "-" + caseId;
        String group = "life-sentinels";
        JobKey jobKey = new JobKey(jobName, group);

        try {
            var jobDetail = JobBuilder.newJob(LifeHeartbeatJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("agent", agent.name())
                    .usingJobData("caseId", caseId.toString())
                    .usingJobData("capabilityName", capabilityName)
                    .build();

            var trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "-trigger", group)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(interval.toMillis())
                            .repeatForever())
                    .startNow()
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new io.casehub.api.spi.ProvisioningException(
                    "Failed to schedule heartbeat for " + capabilityName + ": " + e.getMessage(), e);
        }

        return jobKey;
    }

    private void cancelHeartbeat(JobKey jobKey) {
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            Log.warnf("Failed to cancel heartbeat job %s: %s", jobKey, e.getMessage());
        }
    }
}
