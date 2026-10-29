package io.casehub.life.app.coordination;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CoordinationTemplateValidator {

    private static final String CONNECTOR_PATH_PREFIX = "/api/connectors/";

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
        public static ValidationResult fail(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    @SuppressWarnings("unchecked")
    public ValidationResult validate(String playbookYaml) {
        List<String> errors = new ArrayList<>();

        Map<String, Object> content;
        try {
            content = CoordinationTemplateCatalog.parsePlaybookContent(playbookYaml);
        } catch (Exception e) {
            return ValidationResult.fail(List.of("Malformed YAML: " + e.getMessage()));
        }

        if (!content.containsKey("steps")) {
            return ValidationResult.fail(List.of("Playbook has no steps"));
        }

        try {
            CoordinationTemplateCatalog.compilePlaybook(playbookYaml);
        } catch (Exception e) {
            errors.add("Failed playbook compilation: " + e.getMessage());
        }

        Object stepsObj = content.get("steps");
        if (stepsObj instanceof List<?> steps) {
            for (Object stepObj : steps) {
                if (stepObj instanceof Map<?, ?> step) {
                    Object restReq = step.get("rest-request");
                    if (restReq instanceof Map<?, ?> reqMap) {
                        String url = (String) reqMap.get("url");
                        if (url != null) {
                            validateUrl(url, errors);
                        }
                    }
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    private void validateUrl(String url, List<String> errors) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            errors.add("Rest-request uses external URL: " + url
                + " — only local /api/connectors/* paths are allowed");
            return;
        }
        if (!url.startsWith(CONNECTOR_PATH_PREFIX)) {
            errors.add("Rest-request URL must start with /api/connectors/ — got: " + url);
        }
    }
}
