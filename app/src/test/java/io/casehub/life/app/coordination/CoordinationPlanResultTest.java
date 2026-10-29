package io.casehub.life.app.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.life.api.coordination.CoordinationPlan;
import io.casehub.life.api.coordination.GeneratedTemplate;
import io.casehub.life.api.coordination.WorkerActivation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoordinationPlanResultTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void round_trips_through_json() throws Exception {
        var plan = new CoordinationPlan(
            "contractor-onboarding", Map.of("contractorName", "Acme"),
            "completion-report", Map.of("recipientEmail", "mark@example.com"),
            List.of(new GeneratedTemplate("custom", "steps:\n  - log: test")),
            new WorkerActivation(List.of("get-quotes"), Map.of()));
        String json = MAPPER.writeValueAsString(new CoordinationPlanResult(plan));
        var result = MAPPER.readValue(json, CoordinationPlanResult.class);
        assertThat(result.plan().preTemplate()).isEqualTo("contractor-onboarding");
        assertThat(result.plan().postTemplate()).isEqualTo("completion-report");
        assertThat(result.plan().generatedTemplates()).hasSize(1);
        assertThat(result.plan().generatedTemplates().getFirst().name()).isEqualTo("custom");
        assertThat(result.plan().workerActivation().skipWorkers()).containsExactly("get-quotes");
    }

    @Test
    void handles_null_templates() throws Exception {
        var plan = new CoordinationPlan(null, null, null, null, List.of(), null);
        String json = MAPPER.writeValueAsString(new CoordinationPlanResult(plan));
        var result = MAPPER.readValue(json, CoordinationPlanResult.class);
        assertThat(result.plan().preTemplate()).isNull();
        assertThat(result.plan().postTemplate()).isNull();
        assertThat(result.plan().workerActivation()).isNull();
    }
}
