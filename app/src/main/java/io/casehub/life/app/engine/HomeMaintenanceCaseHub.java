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

import io.casehub.api.engine.YamlCaseHub;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.Worker;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Home maintenance cycle case hub — loads the YAML definition and augments it
 * with in-process worker functions.
 *
 * <p>Workers are lambda functions that run on Quartz worker threads. The two humanTask
 * bindings (approve-contractor, verify-completion) are defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 *
 * <p>Qhorus bridge pattern: the issue-commitment worker is a STUB — in production it would
 * create a qhorus COMMAND on a case-specific channel. The monitor-job binding fires when
 * {@code QhorusMessageSignalBridge} sets {@code .channelMessage} with
 * {@code messageType == "RESPONSE"}. Refs casehub-life#6.
 */
@ApplicationScoped
public class HomeMaintenanceCaseHub extends YamlCaseHub {

    private volatile CaseDefinition augmentedDefinition;

    public HomeMaintenanceCaseHub() {
        super("life/home-maintenance.yaml");
    }

    @Override
    public CaseDefinition getDefinition() {
        if (augmentedDefinition == null) {
            synchronized (this) {
                if (augmentedDefinition == null) {
                    augmentedDefinition = augment(super.getDefinition());
                }
            }
        }
        return augmentedDefinition;
    }

    private CaseDefinition augment(CaseDefinition yaml) {
        yaml.getWorkers().addAll(List.of(
                scheduleInspectionWorker(),
                getQuotesWorker(),
                issueCommitmentWorker(),
                monitorJobWorker(),
                recordCompletionWorker()
        ));
        return yaml;
    }

    private static Capability cap(String name) {
        return Capability.builder().name(name).inputSchema(".").outputSchema(".").build();
    }

    /**
     * Schedules and performs a home inspection. Returns inspection results.
     */
    private Worker scheduleInspectionWorker() {
        return Worker.builder()
                .name("schedule-inspection-agent")
                .capabilities(List.of(cap("schedule-inspection")))
                .function((Map<String, Object> input) -> Map.of(
                        "inspected", true,
                        "condition", "roof needs repair",
                        "inspectionDate", "2026-06-15"
                ))
                .build();
    }

    /**
     * Obtains contractor quotes based on inspection results. Returns 2 quote options.
     */
    private Worker getQuotesWorker() {
        return Worker.builder()
                .name("get-quotes-agent")
                .capabilities(List.of(cap("get-quotes")))
                .function((Map<String, Object> input) -> Map.of(
                        "quoteCount", 2,
                        "quotes", List.of(
                                Map.of("contractor", "ABC Roofing", "amount", 4500, "timeline", "2 weeks"),
                                Map.of("contractor", "XYZ Repairs", "amount", 3800, "timeline", "3 weeks")
                        )
                ))
                .build();
    }

    /**
     * Issues a qhorus COMMAND to the selected contractor (STUB — in production would
     * create a qhorus COMMAND on a case-specific channel and the QhorusMessageSignalBridge
     * would set {@code .channelMessage} when the contractor responds).
     */
    private Worker issueCommitmentWorker() {
        return Worker.builder()
                .name("issue-commitment-agent")
                .capabilities(List.of(cap("issue-commitment")))
                .function((Map<String, Object> input) -> Map.of(
                        "commitmentIssued", true,
                        "channel", "case-stub/contractor"
                ))
                .build();
    }

    /**
     * Monitors job progress after contractor RESPONSE received via QhorusMessageSignalBridge.
     */
    private Worker monitorJobWorker() {
        return Worker.builder()
                .name("monitor-job-agent")
                .capabilities(List.of(cap("monitor-job")))
                .function((Map<String, Object> input) -> Map.of(
                        "progress", "in-progress",
                        "estimatedCompletion", "2026-07-01"
                ))
                .build();
    }

    /**
     * Records job completion to tamper-evident ledger (stub — in production would
     * call LifeLedgerWriter).
     */
    private Worker recordCompletionWorker() {
        return Worker.builder()
                .name("record-completion-agent")
                .capabilities(List.of(cap("record-completion")))
                .function((Map<String, Object> input) -> Map.of(
                        "recorded", true,
                        "ledgerEntryId", "LEDGER-" + System.currentTimeMillis()
                ))
                .build();
    }
}
