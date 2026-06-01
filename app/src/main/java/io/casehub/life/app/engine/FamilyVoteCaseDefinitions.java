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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.HumanTaskTarget;

import java.time.Duration;
import java.util.Set;

/**
 * Fluent Java DSL companion for the family-vote case definition.
 *
 * <p>Produces the same CaseDefinition as the YAML at
 * {@code life/family-vote.yaml} but via the Java builder API. JQ string
 * expressions match the YAML — no lambdas.
 *
 * <p>Useful for programmatic construction in tests or when the YAML parser is
 * not on the classpath.
 */
public final class FamilyVoteCaseDefinitions {

    private FamilyVoteCaseDefinitions() {}

    public static CaseDefinition familyVote() {
        Goal voteCast = Goal.builder()
                .name("vote-cast")
                .kind(GoalKind.SUCCESS)
                .condition(".vote != null")
                .build();

        return CaseDefinition.builder()
                .namespace("life")
                .name("family-vote")
                .version("1.0.0")
                .title("Family vote — single humanTask child case for M-of-N quorum")
                .goals(voteCast)
                .completion(GoalExpression.allOf(voteCast))
                .bindings(
                        Binding.builder()
                                .name("cast-vote")
                                .on(new ContextChangeTrigger("."))
                                .when(".vote == null")
                                .humanTask(HumanTaskTarget.inline()
                                        .title("Cast your vote — approve or reject")
                                        .expiresIn(Duration.ofHours(48))
                                        .candidateGroups(Set.of("household-member"))
                                        .scope("casehubio/life/finance")
                                        .inputMapping("{ proposal: .proposal, estimatedCost: .estimatedCost }")
                                        .outputMapping("{ vote: . }")
                                        .build())
                                .build()
                )
                .build();
    }
}
