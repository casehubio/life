package io.casehub.life.app.coordination;

import io.casehub.engine.flow.PlaybookWorkerFunction;
import io.casehub.platform.compiler.CompilationOutcome;
import io.casehub.platform.compiler.CompilationResult;
import io.casehub.platform.compiler.PlaybookCompiler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.yaml.snakeyaml.Yaml;

@ApplicationScoped
public class CoordinationTemplateCatalog {

    private final Map<String, CoordinationTemplate> classpathTemplates = new ConcurrentHashMap<>();
    private static final PlaybookCompiler COMPILER = new PlaybookCompiler();

    @Inject
    EntityManager em;

    void onStartup(@Observes StartupEvent event) {
        loadClasspathPlaybooks("life/playbooks/");
    }

    @SuppressWarnings("unchecked")
    public void loadClasspathPlaybooks(String basePath) {
        var cl = Thread.currentThread().getContextClassLoader();
        String[] templates = {
            "contractor-onboarding", "appointment-prep", "site-assessment",
            "financial-review-prep", "travel-research", "completion-report",
            "appointment-followup", "payment-and-close",
            "product-search", "order-and-track"
        };
        for (String name : templates) {
            String resourcePath = basePath + name + ".playbook.yaml";
            try (InputStream is = cl.getResourceAsStream(resourcePath)) {
                if (is == null) continue;
                String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                Map<String, Object> content = parsePlaybookContent(yaml);
                String domain = "unknown";
                String description = "";
                if (content.containsKey("meta")) {
                    var meta = (Map<String, Object>) content.get("meta");
                    domain = (String) meta.getOrDefault("domain", "unknown");
                    description = (String) meta.getOrDefault("description", "");
                }

                var template = new CoordinationTemplate(
                    name, domain, yaml, description, true, "system");
                classpathTemplates.put(name, template);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load playbook template: " + resourcePath, e);
            }
        }
    }

    public List<CoordinationTemplate> findByDomain(String domain) {
        var results = new ArrayList<>(classpathTemplates.values().stream()
            .filter(t -> t.getDomain().equals(domain))
            .toList());
        if (em != null) {
            results.addAll(em.createQuery(
                "SELECT t FROM CoordinationTemplate t WHERE t.domain = :domain AND t.curated = false",
                CoordinationTemplate.class)
                .setParameter("domain", domain)
                .getResultList());
        }
        return results;
    }

    public Optional<CoordinationTemplate> findByName(String name) {
        var classpath = classpathTemplates.get(name);
        if (classpath != null) return Optional.of(classpath);
        if (em == null) return Optional.empty();
        return em.createQuery(
            "SELECT t FROM CoordinationTemplate t WHERE t.name = :name",
            CoordinationTemplate.class)
            .setParameter("name", name)
            .getResultStream().findFirst();
    }

    public List<CoordinationTemplate> all() {
        var results = new ArrayList<>(classpathTemplates.values());
        if (em != null) {
            results.addAll(em.createQuery(
                "SELECT t FROM CoordinationTemplate t WHERE t.curated = false",
                CoordinationTemplate.class)
                .getResultList());
        }
        return results;
    }

    @Transactional
    public void save(CoordinationTemplate template) {
        em.persist(template);
    }

    public PlaybookWorkerFunction resolveToPlaybookWorker(String templateName) {
        CoordinationTemplate t = findByName(templateName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown template: " + templateName));
        CompilationResult result = compilePlaybook(t.getTemplateYaml());
        return new PlaybookWorkerFunction(result.workflow(), result);
    }

    public static CompilationResult compilePlaybook(String playbookYaml) {
        Map<String, Object> content = parsePlaybookContent(playbookYaml);
        CompilationOutcome outcome = COMPILER.compile(content);
        if (outcome instanceof CompilationOutcome.Failure f) {
            throw new IllegalArgumentException(
                "Playbook compilation failed: " + f.diagnostics());
        }
        return ((CompilationOutcome.Success) outcome).result();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parsePlaybookContent(String yaml) {
        var loader = new Yaml();
        var documents = new ArrayList<Object>();
        for (Object doc : loader.loadAll(yaml)) {
            documents.add(doc);
        }

        if (documents.size() == 1) {
            return (Map<String, Object>) documents.getFirst();
        }

        if (documents.size() >= 3) {
            var metadata = new LinkedHashMap<>((Map<String, Object>) documents.get(1));
            Object thirdDoc = documents.get(2);
            if (thirdDoc instanceof List<?> stepList) {
                metadata.put("steps", stepList);
            } else if (thirdDoc instanceof Map<?, ?> mapDoc) {
                metadata.putAll((Map<String, Object>) mapDoc);
            }
            return metadata;
        }

        if (documents.size() == 2) {
            var first = (Map<String, Object>) documents.getFirst();
            if (first.containsKey("playbook")) {
                return (Map<String, Object>) documents.get(1);
            }
            var merged = new LinkedHashMap<>(first);
            merged.putAll((Map<String, Object>) documents.get(1));
            return merged;
        }

        throw new IllegalArgumentException("Empty playbook YAML");
    }
}
