package io.casehub.life.app.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordinationTemplateValidatorTest {

    private final CoordinationTemplateValidator validator = new CoordinationTemplateValidator();

    @Test
    void accepts_valid_playbook_with_rest_requests_to_connectors() {
        String yaml = """
            steps:
              - rest-request:
                  method: POST
                  url: /api/connectors/contacts/searchContacts
                  body:
                    query: test
            """;
        var result = validator.validate(yaml);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void accepts_playbook_with_multiple_connector_domains() {
        String yaml = """
            steps:
              - rest-request:
                  method: POST
                  url: /api/connectors/contacts/searchContacts
                  body:
                    query: test
              - rest-request:
                  method: POST
                  url: /api/connectors/location/searchPlaces
                  body:
                    query: test
              - rest-request:
                  method: POST
                  url: /api/connectors/messaging/sendEmail
                  body:
                    subject: test
            """;
        var result = validator.validate(yaml);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void rejects_external_urls() {
        String yaml = """
            steps:
              - rest-request:
                  method: GET
                  url: https://evil.com/steal-data
            """;
        var result = validator.validate(yaml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("external URL"));
    }

    @Test
    void rejects_non_connector_local_paths() {
        String yaml = """
            steps:
              - rest-request:
                  method: DELETE
                  url: /api/life-tasks/all
            """;
        var result = validator.validate(yaml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("/api/connectors/"));
    }

    @Test
    void rejects_yaml_without_steps() {
        var result = validator.validate("not_steps: true");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("no steps"));
    }

    @Test
    void accepts_playbook_without_rest_request_steps() {
        String yaml = """
            steps:
              - log:
                  message: hello
            """;
        var result = validator.validate(yaml);
        assertThat(result.valid()).isTrue();
    }
}
