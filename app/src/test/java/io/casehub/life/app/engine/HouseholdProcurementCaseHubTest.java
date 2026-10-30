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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.HumanRoutingConfig;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.spi.routing.CandidateSetSpec;
import io.casehub.api.spi.routing.StaticSetStrategy;
import io.casehub.life.api.LifeCaseType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

@QuarkusTest
class HouseholdProcurementCaseHubTest {

    @Inject
    HouseholdProcurementCaseHub caseHub;

    @Test
    void definitionLoads() {
        var def = caseHub.getDefinition();
        assertNotNull(def);
        assertEquals("casehub-life", def.getNamespace());
        assertEquals("household-procurement", def.getName());
        assertEquals("1.0.0", def.getVersion());
    }

    @Test
    void hasFiveCapabilities() {
        var names = caseHub.getDefinition().getCapabilities()
                .stream().map(c -> c.name()).toList();
        assertEquals(5, names.size());
        assertTrue(names.containsAll(List.of(
                "analyse-need", "search-products", "fulfil-order",
                "track-delivery", "procurement-sentinel")));
    }

    @Test
    void hasSevenBindings() {
        var names = caseHub.getDefinition().getBindings()
                .stream().map(b -> b.getName()).toList();
        assertEquals(7, names.size());
        assertTrue(names.containsAll(List.of(
                "analyse-need", "search-products", "approve-purchase",
                "fulfil-order", "track-delivery",
                "procurement-sentinel", "sentinel-escalation")));
    }

    @Test
    void approvePurchaseIsJudgment() {
        var binding = caseHub.getDefinition().getBindings().stream()
                .filter(b -> "approve-purchase".equals(b.getName()))
                .findFirst()
                .orElseThrow();
        assertTrue(binding.target() instanceof JudgmentTarget jt
                && "Approve household purchase".equals(jt.title())
                && jt.routingConfig() instanceof HumanRoutingConfig hrc
                && hrc.candidateGroups() instanceof CandidateSetSpec.Inline inline
                && inline.strategy() instanceof StaticSetStrategy ss
                && ss.values().contains("household-admin")
                && "casehubio/life/household".equals(jt.scope()));
    }

    @Test
    void hasOrderFulfilledGoal() {
        var goals = caseHub.getDefinition().getGoals();
        assertEquals(1, goals.size());
        assertEquals("order-fulfilled", goals.get(0).getName());
        assertTrue(goals.get(0).getCondition() instanceof JQExpressionEvaluator jq
                        && jq.expression().contains("deliveryStatus"),
                "Goal condition should check deliveryStatus");
    }

    @Test
    void hasFourWorkers() {
        var workers = caseHub.getDefinition().getWorkers();
        assertEquals(4, workers.size());
        var names = Set.copyOf(workers.stream().map(w -> w.name()).toList());
        assertEquals(Set.of(
                "analyse-need-agent", "search-products-agent",
                "fulfil-order-agent", "track-delivery-agent"), names);
    }

    @Test
    void lifeCaseType() {
        assertEquals(LifeCaseType.HOUSEHOLD_PROCUREMENT, caseHub.lifeCaseType());
    }
}
