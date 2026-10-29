package io.casehub.life.app.coordination;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coordination_template")
public class CoordinationTemplate {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String domain;

    @Column(name = "template_yaml", nullable = false, columnDefinition = "TEXT")
    private String templateYaml;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean curated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Column(name = "tenancy_id", nullable = false)
    private String tenancyId;

    protected CoordinationTemplate() {}

    public CoordinationTemplate(String name, String domain, String templateYaml,
                                String description, boolean curated, String tenancyId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.domain = domain;
        this.templateYaml = templateYaml;
        this.description = description;
        this.curated = curated;
        this.createdAt = Instant.now();
        this.useCount = 0;
        this.tenancyId = tenancyId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDomain() { return domain; }
    public String getTemplateYaml() { return templateYaml; }
    public String getDescription() { return description; }
    public boolean isCurated() { return curated; }
    public Instant getCreatedAt() { return createdAt; }
    public int getUseCount() { return useCount; }
    public String getTenancyId() { return tenancyId; }

    public void incrementUseCount() { this.useCount++; }
}
