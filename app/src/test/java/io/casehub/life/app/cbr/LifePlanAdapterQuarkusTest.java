package io.casehub.life.app.cbr;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
class LifeCbrCbrPlanAdapterQuarkusTest {

    @Inject
    CbrCbrPlanAdapter planAdapter;

    @Test
    void planAdapter_resolvesToLifeCbrCbrPlanAdapter() {
        assertInstanceOf(LifeCbrCbrPlanAdapter.class, planAdapter);
    }
}
