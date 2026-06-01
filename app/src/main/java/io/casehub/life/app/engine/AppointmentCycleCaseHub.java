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
 * Appointment cycle case hub — loads the YAML definition and augments it with
 * in-process worker functions.
 *
 * <p>Workers are lambda functions that run on Quartz worker threads. The humanTask
 * binding (attend-and-record) is defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 *
 * <p>DECLINE pattern: the book-appointment worker returns {@code {declined: true}} when the
 * provider is "unavailable". The find-alternative binding fires on decline and returns a
 * successful alternative booking. Refs casehub-life#6.
 */
@ApplicationScoped
public class AppointmentCycleCaseHub extends YamlCaseHub {

    private volatile CaseDefinition augmentedDefinition;

    public AppointmentCycleCaseHub() {
        super("life/appointment-cycle.yaml");
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
                bookAppointmentWorker(),
                findAlternativeWorker(),
                confirmAppointmentWorker(),
                preVisitPrepWorker(),
                recordHealthDecisionWorker()
        ));
        return yaml;
    }

    private static Capability cap(String name) {
        return Capability.builder().name(name).inputSchema(".").outputSchema(".").build();
    }

    /**
     * Books an appointment. Returns {@code {declined: true}} when provider is "unavailable"
     * to demonstrate the DECLINE recovery path.
     */
    private Worker bookAppointmentWorker() {
        return Worker.builder()
                .name("book-appointment-agent")
                .capabilities(List.of(cap("book-appointment")))
                .function((Map<String, Object> input) -> {
                    String provider = input.get("provider") != null
                            ? String.valueOf(input.get("provider")) : "";
                    if ("unavailable".equalsIgnoreCase(provider)) {
                        return Map.of(
                                "declined", true,
                                "reason", "Provider not accepting new patients"
                        );
                    }
                    return Map.of(
                            "appointmentId", "APT-" + System.currentTimeMillis(),
                            "provider", provider,
                            "confirmed", false
                    );
                })
                .build();
    }

    /**
     * Finds an alternative provider after a decline. Always succeeds in stubs.
     */
    private Worker findAlternativeWorker() {
        return Worker.builder()
                .name("find-alternative-agent")
                .capabilities(List.of(cap("find-alternative")))
                .function((Map<String, Object> input) -> Map.of(
                        "alternativeFound", true,
                        "appointmentId", "APT-ALT-" + System.currentTimeMillis(),
                        "provider", "Dr Alternative",
                        "confirmed", false
                ))
                .build();
    }

    /**
     * Sends appointment confirmation and reminder.
     */
    private Worker confirmAppointmentWorker() {
        return Worker.builder()
                .name("confirm-appointment-agent")
                .capabilities(List.of(cap("confirm-appointment")))
                .function((Map<String, Object> input) -> Map.of(
                        "confirmed", true,
                        "reminderSent", true
                ))
                .build();
    }

    /**
     * Sends pre-visit checklist and preparation instructions.
     */
    private Worker preVisitPrepWorker() {
        return Worker.builder()
                .name("pre-visit-prep-agent")
                .capabilities(List.of(cap("pre-visit-prep")))
                .function((Map<String, Object> input) -> Map.of(
                        "checklistSent", true,
                        "instructions", "Bring ID, insurance card, list of medications"
                ))
                .build();
    }

    /**
     * Records health decision to tamper-evident ledger (stub — in production would
     * call LifeLedgerWriter).
     */
    private Worker recordHealthDecisionWorker() {
        return Worker.builder()
                .name("record-health-decision-agent")
                .capabilities(List.of(cap("record-health-decision")))
                .function((Map<String, Object> input) -> Map.of(
                        "recorded", true,
                        "ledgerEntryId", "LEDGER-" + System.currentTimeMillis()
                ))
                .build();
    }
}
