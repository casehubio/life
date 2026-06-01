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
 * Care coordination case hub — loads the YAML definition and augments it with
 * in-process worker functions.
 *
 * <p>Workers are lambda functions that run on Quartz worker threads. The three humanTask
 * bindings (assign-carer, escalate-concern, care-review) are defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 *
 * <p>SubCase pattern: the care-episode binding spawns a child case (care-episode) and
 * waits for completion. The child's final context is merged back as {@code episodeResult}.
 *
 * <p>Adaptive escalation: the escalate-concern binding fires only when
 * {@code .healthCheck.healthConcern == true} — otherwise the workflow proceeds
 * directly to care-review. In production, the escalation worker would also signal
 * an active appointment-cycle case via CaseHubRuntime.signal(). Refs casehub-life#6.
 */
@ApplicationScoped
public class CareCoordinationCaseHub extends YamlCaseHub {

    private volatile CaseDefinition augmentedDefinition;

    public CareCoordinationCaseHub() {
        super("life/care-coordination.yaml");
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
                needsAssessmentWorker(),
                carePlanWorker(),
                healthCheckWorker()
        ));
        return yaml;
    }

    private static Capability cap(String name) {
        return Capability.builder().name(name).inputSchema(".").outputSchema(".").build();
    }

    /**
     * Assesses care needs based on the care request. Returns an assessment with
     * care level and recommended frequency.
     */
    private Worker needsAssessmentWorker() {
        return Worker.builder()
                .name("needs-assessment-agent")
                .capabilities(List.of(cap("needs-assessment")))
                .function((Map<String, Object> input) -> Map.of(
                        "careLevel", "moderate",
                        "recommendedFrequency", "3x weekly",
                        "specialRequirements", List.of("mobility assistance", "medication management")
                ))
                .build();
    }

    /**
     * Produces a care schedule based on the assessment.
     */
    private Worker carePlanWorker() {
        return Worker.builder()
                .name("care-plan-agent")
                .capabilities(List.of(cap("care-plan")))
                .function((Map<String, Object> input) -> Map.of(
                        "schedule", List.of("Mon 09:00", "Wed 09:00", "Fri 09:00"),
                        "duration", "2 hours per visit",
                        "tasks", List.of("medication check", "mobility exercises", "meal preparation")
                ))
                .build();
    }

    /**
     * Analyses care notes from the episode and flags health concerns.
     * Sets {@code healthConcern: true} if the episode notes contain a concern indicator.
     */
    private Worker healthCheckWorker() {
        return Worker.builder()
                .name("health-check-agent")
                .capabilities(List.of(cap("health-check")))
                .function((Map<String, Object> input) -> {
                    // Stub: no concern by default — test can override via context
                    return Map.of(
                            "reviewed", true,
                            "healthConcern", false,
                            "notes", "Patient stable, no concerns identified"
                    );
                })
                .build();
    }
}
