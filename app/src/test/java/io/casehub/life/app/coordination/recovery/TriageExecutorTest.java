package io.casehub.life.app.coordination.recovery;

import io.casehub.life.app.coordination.CoordinationTemplate;
import io.casehub.life.app.coordination.CoordinationTemplateCatalog;
import io.casehub.life.app.coordination.CoordinationTemplateValidator;
import io.casehub.life.app.coordination.CoordinationTemplateValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TriageExecutorTest {

    private CoordinationTemplateCatalog catalog;
    private CoordinationTemplateValidator validator;
    private TriageExecutor executor;

    @BeforeEach
    void setUp() {
        catalog = Mockito.mock(CoordinationTemplateCatalog.class);
        validator = Mockito.mock(CoordinationTemplateValidator.class);
        executor = new TriageExecutor(catalog, validator);
    }

    @Test
    void retryModifiedReturnsParamsToInject() {
        var decision = new TriageDecision(
            TriageAction.RETRY_MODIFIED, "increase timeout",
            Map.of("timeout", 30), null, null, null, null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.RETRY_MODIFIED);
        assertThat(outcome.modifiedParams()).containsEntry("timeout", 30);
        assertThat(outcome.shouldReschedule()).isTrue();
    }

    @Test
    void switchPlaybookResolvesFromCatalog() {
        var template = Mockito.mock(CoordinationTemplate.class);
        Mockito.when(catalog.findByName("alt-template")).thenReturn(Optional.of(template));

        var decision = new TriageDecision(
            TriageAction.SWITCH_PLAYBOOK, "try alternate",
            null, "alt-template", null, null, null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.SWITCH_PLAYBOOK);
        assertThat(outcome.appliedTemplateName()).isEqualTo("alt-template");
        assertThat(outcome.shouldReschedule()).isTrue();
    }

    @Test
    void switchPlaybookFallsBackWhenNotFound() {
        Mockito.when(catalog.findByName("missing")).thenReturn(Optional.empty());

        var decision = new TriageDecision(
            TriageAction.SWITCH_PLAYBOOK, "try alternate",
            null, "missing", null, null, null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.FALLBACK_MCP);
        assertThat(outcome.shouldReschedule()).isFalse();
    }

    @Test
    void generateFixValidatesAndPersists() {
        Mockito.when(validator.validate(Mockito.anyString()))
            .thenReturn(ValidationResult.ok());
        Mockito.when(catalog.findByName("fixed-template")).thenReturn(Optional.empty());

        var decision = new TriageDecision(
            TriageAction.GENERATE_FIX, "add pre-check",
            null, null, "steps:\n  - name: pre-check", "fixed-template", null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.GENERATE_FIX);
        assertThat(outcome.appliedTemplateName()).isEqualTo("fixed-template");
        Mockito.verify(catalog).save(Mockito.any(CoordinationTemplate.class));
    }

    @Test
    void generateFixFallsBackOnInvalidYaml() {
        Mockito.when(validator.validate(Mockito.anyString()))
            .thenReturn(ValidationResult.fail(List.of("missing steps")));

        var decision = new TriageDecision(
            TriageAction.GENERATE_FIX, "bad fix",
            null, null, "invalid yaml", "bad-template", null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.FALLBACK_MCP);
    }

    @Test
    void generateFixRenamesWhenCurated() {
        Mockito.when(validator.validate(Mockito.anyString()))
            .thenReturn(ValidationResult.ok());
        var curatedTemplate = Mockito.mock(CoordinationTemplate.class);
        Mockito.when(curatedTemplate.isCurated()).thenReturn(true);
        Mockito.when(curatedTemplate.getDomain()).thenReturn("CONTRACTOR_COORDINATION");
        Mockito.when(catalog.findByName("contractor-onboarding")).thenReturn(Optional.of(curatedTemplate));

        var decision = new TriageDecision(
            TriageAction.GENERATE_FIX, "fix curated",
            null, null, "steps:\n  - name: fixed", "contractor-onboarding", null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.appliedTemplateName()).isEqualTo("contractor-onboarding-fixed");
    }

    @Test
    void generateFixFallsBackOnMissingYaml() {
        var decision = new TriageDecision(
            TriageAction.GENERATE_FIX, "missing yaml",
            null, null, null, "some-name", null);

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.FALLBACK_MCP);
    }

    @Test
    void fallbackMcpReturnsNoReschedule() {
        var decision = new TriageDecision(
            TriageAction.FALLBACK_MCP, "give up",
            null, null, null, null, "all attempts exhausted");

        TriageOutcome outcome = executor.apply(decision, UUID.randomUUID());
        assertThat(outcome.action()).isEqualTo(TriageAction.FALLBACK_MCP);
        assertThat(outcome.shouldReschedule()).isFalse();
    }
}
