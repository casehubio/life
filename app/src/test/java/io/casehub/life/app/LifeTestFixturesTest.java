package io.casehub.life.app;

import io.casehub.work.api.WorkItemPriority;
import io.casehub.work.runtime.model.WorkItemTemplate;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class LifeTestFixturesTest {

    @Inject EntityManager em;

    private long templateCount(String name) {
        return em.createQuery("SELECT COUNT(t) FROM WorkItemTemplate t WHERE t.name = ?1", Long.class)
                .setParameter(1, name).getSingleResult();
    }

    private WorkItemTemplate templateByName(String name) {
        return em.createQuery("FROM WorkItemTemplate WHERE name = ?1", WorkItemTemplate.class)
                .setParameter(1, name).getResultStream().findFirst().orElse(null);
    }

    @Test
    @Transactional
    void seedStandardTemplates_createsThreeTemplates() {
        LifeTestFixtures.seedStandardTemplates();

        assertThat(templateCount("household-task")).isEqualTo(1);
        assertThat(templateCount("health-appointment")).isEqualTo(1);
        assertThat(templateCount("contractor-coordination")).isEqualTo(1);

        WorkItemTemplate t = templateByName("household-task");
        assertThat(t.candidateGroups).isEqualTo("household-member");
        assertThat(t.priority).isEqualTo(WorkItemPriority.MEDIUM);
        assertThat(t.defaultExpiryHours).isEqualTo(24);
        assertThat(t.createdBy).isEqualTo("life-system");
    }

    @Test
    @Transactional
    void seedEscalationTemplate_createsHighPriorityAdminTemplate() {
        LifeTestFixtures.seedEscalationTemplate();

        WorkItemTemplate t = templateByName("life-escalation");
        assertThat(t).isNotNull();
        assertThat(t.candidateGroups).isEqualTo("household-admin");
        assertThat(t.priority).isEqualTo(WorkItemPriority.HIGH);
        assertThat(t.description).isNotNull().isNotBlank();
    }

    @Test
    @Transactional
    void seedStandardTemplates_isIdempotent() {
        LifeTestFixtures.seedStandardTemplates();
        LifeTestFixtures.seedStandardTemplates();

        assertThat(templateCount("household-task")).isEqualTo(1);
        assertThat(templateCount("health-appointment")).isEqualTo(1);
        assertThat(templateCount("contractor-coordination")).isEqualTo(1);
    }

    @Test
    @Transactional
    void seedEscalationTemplate_isIdempotent() {
        LifeTestFixtures.seedEscalationTemplate();
        LifeTestFixtures.seedEscalationTemplate();

        assertThat(templateCount("life-escalation")).isEqualTo(1);
    }
}
