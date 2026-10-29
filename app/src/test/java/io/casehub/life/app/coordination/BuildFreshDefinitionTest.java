package io.casehub.life.app.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.flow.PlaybookWorkerFunction;
import io.casehub.life.api.coordination.CoordinationPlan;
import io.casehub.life.api.coordination.WorkerActivation;
import io.casehub.life.app.engine.LifeTypedCaseHub;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildFreshDefinitionTest {

    private CoordinationTemplateCatalog catalog;
    private CoordinationTemplateValidator validator;

    @BeforeEach
    void setUp() {
        catalog = new CoordinationTemplateCatalog();
        catalog.loadClasspathPlaybooks("life/playbooks/");
        validator = new CoordinationTemplateValidator();
    }

    private CaseDefinition makeBaseDefinition() {
        return CaseDefinition.builder()
            .namespace("life").name("test-case").version("1.0.0")
            .worker(Worker.builder()
                .name("worker-a-agent").capabilityName("worker-a")
                .function(WorkerFunction.NONE).build())
            .worker(Worker.builder()
                .name("worker-b-agent").capabilityName("worker-b")
                .function(WorkerFunction.NONE).build())
            .build();
    }

    @Test
    void adds_pre_and_post_playbook_workers_when_templates_selected() {
        var base = makeBaseDefinition();
        var plan = new CoordinationPlan(
            "contractor-onboarding", Map.of(),
            "completion-report", Map.of(),
            List.of(), null);

        var fresh = LifeTypedCaseHub.buildFreshDefinition(base, plan, catalog, validator);

        assertThat(fresh.getWorkers()).hasSizeGreaterThan(2);
        assertThat(fresh.getWorkers().getFirst().name()).isEqualTo("pre-stage-data");
        assertThat(fresh.getWorkers().getFirst().function()).isInstanceOf(PlaybookWorkerFunction.class);
        assertThat(fresh.getWorkers().getLast().name()).isEqualTo("post-process-results");
        assertThat(fresh.getWorkers().getLast().function()).isInstanceOf(PlaybookWorkerFunction.class);

        var domainWorkers = fresh.getWorkers().subList(1, fresh.getWorkers().size() - 1);
        assertThat(domainWorkers).extracting(Worker::name)
            .containsExactly("worker-a-agent", "worker-b-agent");
    }

    @Test
    void skips_flow_workers_when_no_templates_selected() {
        var base = makeBaseDefinition();
        var plan = new CoordinationPlan(null, null, null, null, List.of(), null);

        var fresh = LifeTypedCaseHub.buildFreshDefinition(base, plan, catalog, validator);

        assertThat(fresh.getWorkers()).hasSize(2);
        assertThat(fresh.getWorkers()).extracting(Worker::name)
            .containsExactly("worker-a-agent", "worker-b-agent");
    }

    @Test
    void applies_skip_workers_from_worker_activation() {
        var base = makeBaseDefinition();
        var activation = new WorkerActivation(List.of("worker-a-agent"), Map.of());
        var plan = new CoordinationPlan(null, null, null, null, List.of(), activation);

        var fresh = LifeTypedCaseHub.buildFreshDefinition(base, plan, catalog, validator);

        assertThat(fresh.getWorkers()).hasSize(1);
        assertThat(fresh.getWorkers().getFirst().name()).isEqualTo("worker-b-agent");
    }

    @Test
    void does_not_modify_base_definition() {
        var base = makeBaseDefinition();
        int originalSize = base.getWorkers().size();
        var plan = new CoordinationPlan(
            "contractor-onboarding", Map.of(),
            "completion-report", Map.of(),
            List.of(), null);

        LifeTypedCaseHub.buildFreshDefinition(base, plan, catalog, validator);

        assertThat(base.getWorkers()).hasSize(originalSize);
    }
}
