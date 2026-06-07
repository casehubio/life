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
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.api.model.SubCase;
import io.casehub.api.model.Worker;
import io.casehub.api.model.WorkerResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Travel plan case hub — loads the YAML definition and augments it with
 * in-process worker functions and M-of-N SubCase bindings.
 *
 * <p>Workers are lambda functions that run on Quartz worker threads. The humanTask
 * binding (approval-gate) is defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 *
 * <p>M-of-N SubCase bindings (family-vote-a/b/c) are added in Java augmentation
 * because the YAML SubCase schema does not support {@code groupId},
 * {@code totalInGroup}, {@code requiredCount}, or {@code onThresholdReached} fields.
 *
 * <p>DECLINE pattern: the booking worker returns {@code {declined: true}} when the
 * context has {@code simulateDecline == true}. The rebooking binding fires on decline
 * and returns an alternative booking. Refs casehub-life#6.
 */
@ApplicationScoped
public class TravelPlanCaseHub extends YamlCaseHub {

    private volatile CaseDefinition augmentedDefinition;

    public TravelPlanCaseHub() {
        super("life/travel-plan.yaml");
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
        // Add workers for all capability bindings
        yaml.getWorkers().addAll(List.of(
                destinationResearchWorker(),
                flightSearchWorker(),
                hotelSearchWorker(),
                budgetAssessmentWorker(),
                bookingWorker(),
                rebookingWorker(),
                confirmationWorker()
        ));

        // Add M-of-N SubCase bindings — YAML schema does not support these fields
        yaml.getBindings().addAll(List.of(
                familyVoteBinding("family-vote-a"),
                familyVoteBinding("family-vote-b"),
                familyVoteBinding("family-vote-c")
        ));

        return yaml;
    }

    /**
     * Creates a family-vote SubCase binding with M-of-N quorum: 2-of-3, KEEP on threshold.
     * All three bindings share the same condition and groupId — the engine coordinates
     * quorum across the group.
     */
    private Binding familyVoteBinding(String name) {
        return Binding.builder()
                .name(name)
                .on(new ContextChangeTrigger("."))
                .when(".budgetAssessment != null and .budgetAssessment.isHighValue == true and .familyVoteResult == null")
                .subCase(SubCase.builder()
                        .namespace("life")
                        .name("family-vote")
                        .version("1.0.0")
                        .groupId("family-vote")
                        .totalInGroup(3)
                        .requiredCount(2)
                        .onThresholdReached(OnThresholdReached.KEEP)
                        .inputMapping("{ proposal: .selectedDestination, estimatedCost: .budgetAssessment.totalCost }")
                        .outputMapping("{ familyVoteResult: . }")
                        .build())
                .build();
    }

    private static Capability cap(String name) {
        return Capability.builder().name(name).inputSchema(".").outputSchema(".").build();
    }

    /**
     * Researches destination options. Returns 3 options with costs.
     */
    private Worker destinationResearchWorker() {
        return Worker.builder()
                .name("destination-research-agent")
                .capabilities(List.of(cap("destination-research")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "options", List.of(
                                Map.of("name", "Barcelona", "estimatedCost", 1800),
                                Map.of("name", "Tokyo", "estimatedCost", 4500),
                                Map.of("name", "Reykjavik", "estimatedCost", 6200)
                        )
                )))
                .build();
    }

    /**
     * Searches available flights for selected destination.
     */
    private Worker flightSearchWorker() {
        return Worker.builder()
                .name("flight-search-agent")
                .capabilities(List.of(cap("flight-search")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "flights", List.of(
                                Map.of("airline", "BA", "price", 450, "duration", "2h30m"),
                                Map.of("airline", "Ryanair", "price", 180, "duration", "3h15m")
                        )
                )))
                .build();
    }

    /**
     * Searches available hotels for selected destination.
     */
    private Worker hotelSearchWorker() {
        return Worker.builder()
                .name("hotel-search-agent")
                .capabilities(List.of(cap("hotel-search")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "hotels", List.of(
                                Map.of("name", "Grand Hotel", "pricePerNight", 120, "rating", 4.5),
                                Map.of("name", "Budget Inn", "pricePerNight", 55, "rating", 3.8)
                        )
                )))
                .build();
    }

    /**
     * Assesses total cost. Sets {@code requiresApproval} (true if over 2000) and
     * {@code isHighValue} (true if over 5000).
     */
    private Worker budgetAssessmentWorker() {
        return Worker.builder()
                .name("budget-assessment-agent")
                .capabilities(List.of(cap("budget-assessment")))
                .function((Map<String, Object> input) -> {
                    int totalCost = 3500; // stub mid-range cost
                    return WorkerResult.of(Map.of(
                            "totalCost", totalCost,
                            "requiresApproval", totalCost > 2000,
                            "isHighValue", totalCost > 5000
                    ));
                })
                .build();
    }

    /**
     * Books selected flights and hotels. Returns {@code {declined: true}} when the
     * context has {@code simulateDecline == true} to demonstrate DECLINE recovery.
     */
    private Worker bookingWorker() {
        return Worker.builder()
                .name("booking-agent")
                .capabilities(List.of(cap("booking")))
                .function((Map<String, Object> input) -> {
                    Object simulateDecline = input.get("simulateDecline");
                    if (Boolean.TRUE.equals(simulateDecline)) {
                        return WorkerResult.of(Map.of(
                                "declined", true,
                                "reason", "No availability for selected dates"
                        ));
                    }
                    return WorkerResult.of(Map.of(
                            "bookingRef", "TRV-" + System.currentTimeMillis(),
                            "status", "confirmed"
                    ));
                })
                .build();
    }

    /**
     * Rebooks with alternative dates after a DECLINE.
     */
    private Worker rebookingWorker() {
        return Worker.builder()
                .name("rebooking-agent")
                .capabilities(List.of(cap("rebooking")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "bookingRef", "TRV-ALT-" + System.currentTimeMillis(),
                        "status", "confirmed",
                        "alternativeDates", true
                )))
                .build();
    }

    /**
     * Confirms booking and sends itinerary.
     */
    private Worker confirmationWorker() {
        return Worker.builder()
                .name("confirmation-agent")
                .capabilities(List.of(cap("confirmation")))
                .function((Map<String, Object> input) -> WorkerResult.of(Map.of(
                        "confirmed", true,
                        "itinerarySent", true,
                        "confirmationRef", "CONF-" + System.currentTimeMillis()
                )))
                .build();
    }
}
