package io.casehub.life.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LifeCaseStatusTest {
    @Test
    void threeStatusesExist() {
        assertThat(LifeCaseStatus.values()).hasSize(3);
        assertThat(LifeCaseStatus.valueOf("ACTIVE")).isNotNull();
        assertThat(LifeCaseStatus.valueOf("COMPLETED")).isNotNull();
        assertThat(LifeCaseStatus.valueOf("FAILED")).isNotNull();
    }
}
