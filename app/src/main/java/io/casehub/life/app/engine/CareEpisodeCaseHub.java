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
import io.casehub.api.model.WorkerResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Care episode case hub — child case spawned by care-coordination via SubCase binding.
 *
 * <p>Not injected by {@link LifeCaseService} — only spawned as a sub-case by the
 * care-coordination case. Registered as a CDI bean so the engine can discover it.
 *
 * <p>Workers are lambda functions that run on Quartz worker threads. The humanTask
 * binding (record-notes) is defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 *
 * <p>Refs casehub-life#6.
 */
@ApplicationScoped
public class CareEpisodeCaseHub extends YamlCaseHub {

    private volatile CaseDefinition augmentedDefinition;

    public CareEpisodeCaseHub() {
        super("life/care-episode.yaml");
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
                assessPatientWorker(),
                provideCareWorker()
        ));
        return yaml;
    }

    private static Capability cap(String name) {
        return Capability.builder().name(name).inputSchema(".").outputSchema(".").build();
    }

    /**
     * Assesses patient condition at start of care episode.
     */
    private Worker assessPatientWorker() {
        return Worker.builder()
                .name("assess-patient-agent")
                .capabilities(List.of(cap("assess-patient")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "vitalSigns", Map.of("bloodPressure", "130/85", "heartRate", 72, "temperature", 36.8),
                        "mobility", "assisted",
                        "cognition", "alert and oriented"
                )))
                .build();
    }

    /**
     * Provides care based on patient assessment and care plan.
     */
    private Worker provideCareWorker() {
        return Worker.builder()
                .name("provide-care-agent")
                .capabilities(List.of(cap("provide-care")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "tasksCompleted", List.of("medication administered", "mobility exercises done", "meal prepared"),
                        "duration", "2 hours",
                        "observations", "Patient in good spirits, appetite normal"
                )))
                .build();
    }
}
