package io.casehub.life.app.coordination.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TriageDecisionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void roundTripsViaJson() throws Exception {
        var decision = new TriageDecision(
            TriageAction.RETRY_MODIFIED,
            "connector timeout — increase timeout param",
            Map.of("timeout", 30),
            null, null, null, null);

        String json = MAPPER.writeValueAsString(decision);
        TriageDecision deserialized = MAPPER.readValue(json, TriageDecision.class);

        assertThat(deserialized.action()).isEqualTo(TriageAction.RETRY_MODIFIED);
        assertThat(deserialized.modifiedParams()).containsEntry("timeout", 30);
    }

    @Test
    void eachActionHasCorrectFields() {
        var switchDecision = new TriageDecision(
            TriageAction.SWITCH_PLAYBOOK, "try alternate",
            null, "contractor-onboarding-v2", null, null, null);
        assertThat(switchDecision.alternateTemplateName()).isEqualTo("contractor-onboarding-v2");

        var fixDecision = new TriageDecision(
            TriageAction.GENERATE_FIX, "add pre-check step",
            null, null, "steps:\n  - name: pre-check", "contractor-onboarding-fixed", null);
        assertThat(fixDecision.fixedPlaybookYaml()).isNotEmpty();

        var fallbackDecision = new TriageDecision(
            TriageAction.FALLBACK_MCP, "no viable playbook",
            null, null, null, null, "all templates failed for this contractor");
        assertThat(fallbackDecision.fallbackReason()).contains("all templates");
    }
}
