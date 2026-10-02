package io.casehub.life.app.cbr;

import io.casehub.neocortex.memory.cbr.CbrPlanAdapter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
class LifeCbrPlanAdapterQuarkusTest {

    @Inject
    CbrPlanAdapter planAdapter;

    @Test
    void planAdapter_resolvesToLifePlanAdapter() {
        assertInstanceOf(LifePlanAdapter.class, planAdapter);
    }
}
