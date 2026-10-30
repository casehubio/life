package io.casehub.life.app.coordination.recovery;

import io.casehub.life.app.coordination.CoordinationTemplate;
import io.casehub.life.app.coordination.CoordinationTemplateCatalog;
import io.casehub.life.app.coordination.CoordinationTemplateValidator;
import io.casehub.life.app.coordination.CoordinationTemplateValidator.ValidationResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class TriageExecutor {

    private static final Logger LOG = Logger.getLogger(TriageExecutor.class);

    private final CoordinationTemplateCatalog catalog;
    private final CoordinationTemplateValidator validator;

    public TriageExecutor(CoordinationTemplateCatalog catalog,
                          CoordinationTemplateValidator validator) {
        this.catalog = catalog;
        this.validator = validator;
    }

    public TriageOutcome apply(TriageDecision decision, UUID caseId) {
        return switch (decision.action()) {
            case RETRY_MODIFIED -> applyRetryModified(decision);
            case SWITCH_PLAYBOOK -> applySwitchPlaybook(decision);
            case GENERATE_FIX -> applyGenerateFix(decision);
            case FALLBACK_MCP -> TriageOutcome.fallback();
        };
    }

    private TriageOutcome applyRetryModified(TriageDecision decision) {
        return TriageOutcome.reschedule(
            TriageAction.RETRY_MODIFIED, decision.modifiedParams(), null);
    }

    private TriageOutcome applySwitchPlaybook(TriageDecision decision) {
        String altName = decision.alternateTemplateName();
        var template = catalog.findByName(altName);
        if (template.isEmpty()) {
            LOG.warnf("Triage SWITCH_PLAYBOOK: template '%s' not found, falling back to MCP", altName);
            return TriageOutcome.fallback();
        }
        return TriageOutcome.reschedule(TriageAction.SWITCH_PLAYBOOK, Map.of(), altName);
    }

    private TriageOutcome applyGenerateFix(TriageDecision decision) {
        String yaml = decision.fixedPlaybookYaml();
        String name = decision.fixedPlaybookName();
        if (yaml == null || name == null) {
            LOG.warn("Triage GENERATE_FIX: missing YAML or name, falling back to MCP");
            return TriageOutcome.fallback();
        }

        ValidationResult validation = validator.validate(yaml);
        if (!validation.valid()) {
            LOG.warnf("Triage GENERATE_FIX: invalid YAML — %s, falling back to MCP", validation.errors());
            return TriageOutcome.fallback();
        }

        var existing = catalog.findByName(name);
        if (existing.isPresent() && existing.get().isCurated()) {
            String fixedName = name + "-fixed";
            LOG.infof("Triage: curated template '%s' — saving fix as '%s'", name, fixedName);
            name = fixedName;
        }

        var domainFromExisting = existing.map(CoordinationTemplate::getDomain).orElse("unknown");
        var template = new CoordinationTemplate(
            name, domainFromExisting, yaml,
            "Auto-generated fix via triage round", false, "life-personal");
        catalog.save(template);

        return TriageOutcome.reschedule(TriageAction.GENERATE_FIX, Map.of(), name);
    }
}
