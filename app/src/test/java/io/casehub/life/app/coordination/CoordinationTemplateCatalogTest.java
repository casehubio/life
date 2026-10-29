package io.casehub.life.app.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.platform.compiler.CompilationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoordinationTemplateCatalogTest {

    private CoordinationTemplateCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new CoordinationTemplateCatalog();
        catalog.loadClasspathPlaybooks("life/playbooks/");
    }

    @Test
    void finds_classpath_template_by_name() {
        var result = catalog.findByName("contractor-onboarding");
        assertThat(result).isPresent();
        assertThat(result.get().getDomain()).isEqualTo("contractor-coordination");
        assertThat(result.get().isCurated()).isTrue();
    }

    @Test
    void finds_templates_by_domain() {
        var results = catalog.findByDomain("contractor-coordination");
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(t -> t.getName().equals("contractor-onboarding"));
    }

    @Test
    void loads_all_eight_curated_templates() {
        var all = catalog.all();
        assertThat(all).hasSize(8);
    }

    @Test
    void resolves_template_to_playbook_worker_function() {
        var pwf = catalog.resolveToPlaybookWorker("contractor-onboarding");
        assertThat(pwf).isNotNull();
        assertThat(pwf.workflow()).isNotNull();
        assertThat(pwf.workflow().getDo()).isNotEmpty();
        assertThat(pwf.compilationResult()).isNotNull();
    }

    @Test
    void compile_playbook_produces_valid_compilation_result() {
        String yaml = """
            steps:
              - rest-request:
                  method: GET
                  url: /api/connectors/contacts/searchContacts?query=test
            """;
        CompilationResult result = CoordinationTemplateCatalog.compilePlaybook(yaml);
        assertThat(result).isNotNull();
        assertThat(result.workflow()).isNotNull();
        assertThat(result.workflow().getDo()).hasSize(1);
    }

    @Test
    void throws_for_unknown_template() {
        assertThatThrownBy(() -> catalog.resolveToPlaybookWorker("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown template");
    }

    @Test
    void returns_empty_for_unknown_name() {
        assertThat(catalog.findByName("nonexistent")).isEmpty();
    }
}
