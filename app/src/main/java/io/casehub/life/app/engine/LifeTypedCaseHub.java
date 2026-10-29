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
import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.engine.flow.PlaybookWorkerFunction;
import io.casehub.life.api.LifeCaseType;
import io.casehub.life.api.coordination.CoordinationPlan;
import io.casehub.life.app.cbr.CbrInputTransformer;
import io.casehub.life.app.cbr.LifeCbrExperienceFormatter;
import io.casehub.life.app.coordination.CoordinationTemplateCatalog;
import io.casehub.life.app.coordination.CoordinationTemplateValidator;
import io.casehub.life.app.engine.agent.LifeAgentDescriptorFactory;
import io.casehub.life.app.engine.agent.LifeAgentProviderFactory;
import io.casehub.platform.compiler.CompilationResult;
import io.casehub.worker.api.Worker;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Template method base class for Life case definitions backed by YAML and LLM agents.
 * Subclasses override {@link #configureCase(CaseDefinition)} to add workers and bindings.
 * AgentDescriptor registration is handled automatically by {@link #augment(CaseDefinition)}.
 * Use {@link #agentWorker(String, String, Class)} for standard LLM-backed workers.
 */
public abstract class LifeTypedCaseHub extends YamlCaseHub {

    static final  String              CBR_SYSTEM_PROMPT_SUFFIX = "If a _cbrContext section is present in the input, it contains summaries of similar past cases. Use these to calibrate your response — adjust cost estimates, timeline predictions, and risk assessments based on historical patterns. If no _cbrContext is present, proceed with your best judgment.";
    private final LifeAgent           agent;
    @Inject
    LifeAgentProviderFactory agentProviderFactory;
    @Inject
    LifeAgentDescriptorFactory descriptorFactory;
    @Inject
    LifeCbrExperienceFormatter cbrFormatter;
    @Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    CbrInputTransformer cbrInputTransformer;


    protected LifeTypedCaseHub(String path, LifeAgent agent) {
        super(path);
        this.agent = agent;
    }

    public abstract LifeCaseType lifeCaseType();

    protected LifeAgent agent() {
        return agent;
    }

    @Override
    protected final void augment(CaseDefinition definition) {
        configureCase(definition);
        definition.setAgentDescriptors(Map.of(
                agent.agentId(), descriptorFactory.descriptorFor(agent)));
    }

    protected abstract void configureCase(CaseDefinition definition);

    protected Worker agentWorker(String capabilityName, String systemPrompt,
                                 Class<?> responseSchema) {
        WorkerAgentInvoker a = agentProviderFactory.invokerFor(agent,
                systemPrompt + "\n\n" + CBR_SYSTEM_PROMPT_SUFFIX,
                null, responseSchema,
                cbrInputTransformer);
        return Worker.builder()
                     .name(capabilityName + "-agent")
                     .capabilityName(capabilityName)
                     .function(new AgentWorkerFunction(a))
                     .build();
    }

    @jakarta.annotation.PostConstruct
    void initCbrTransformer() {
        this.cbrInputTransformer = new CbrInputTransformer(cbrFormatter, objectMapper);
    }

    public static CaseDefinition buildFreshDefinition(
            CaseDefinition base,
            CoordinationPlan plan,
            CoordinationTemplateCatalog catalog,
            CoordinationTemplateValidator validator) {
        var workers = new ArrayList<>(base.getWorkers());

        if (plan.workerActivation() != null && plan.workerActivation().skipWorkers() != null) {
            workers.removeIf(w ->
                plan.workerActivation().skipWorkers().contains(w.name()));
        }

        if (plan.preTemplate() != null) {
            PlaybookWorkerFunction preWf = catalog.resolveToPlaybookWorker(plan.preTemplate());
            workers.addFirst(Worker.builder()
                .name("pre-stage-data")
                .capabilityName("mechanical-pre-staging")
                .function(preWf)
                .build());
        }

        if (plan.postTemplate() != null) {
            PlaybookWorkerFunction postWf = catalog.resolveToPlaybookWorker(plan.postTemplate());
            workers.add(Worker.builder()
                .name("post-process-results")
                .capabilityName("mechanical-post-processing")
                .function(postWf)
                .build());
        }

        if (plan.generatedTemplates() != null) {
            for (var gen : plan.generatedTemplates()) {
                var validation = validator.validate(gen.templateYaml());
                if (validation.valid()) {
                    CompilationResult result = CoordinationTemplateCatalog.compilePlaybook(gen.templateYaml());
                    workers.add(Worker.builder()
                        .name(gen.name())
                        .capabilityName(gen.name())
                        .function(new PlaybookWorkerFunction(result.workflow(), result))
                        .build());
                }
            }
        }

        CaseDefinition fresh = CaseDefinition.builder()
            .namespace(base.getNamespace())
            .name(base.getName())
            .version(base.getVersion())
            .title(base.getTitle())
            .summary(base.getSummary())
            .workers(workers)
            .capabilities(base.getCapabilities())
            .bindings(base.getBindings())
            .milestones(base.getMilestones())
            .goals(base.getGoals())
            .semanticData(base.getSemanticData())
            .build();

        fresh.setDsl(base.getDsl());
        fresh.setCompletion(base.getCompletion());
        return fresh;
    }
}

