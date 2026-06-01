package io.casehub.life.app.observer;

import io.casehub.life.api.LifeDomain;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LifeDecisionLedgerObserverScopeFallbackTest {

    @Test
    void extractsDomainFromHierarchicalScope() {
        assertThat(LifeDecisionLedgerObserver.domainFromScope("casehubio/life/health"))
                .isEqualTo(LifeDomain.HEALTH);
        assertThat(LifeDecisionLedgerObserver.domainFromScope("casehubio/life/finance"))
                .isEqualTo(LifeDomain.FINANCE);
        assertThat(LifeDecisionLedgerObserver.domainFromScope("casehubio/life/legal"))
                .isEqualTo(LifeDomain.LEGAL);
        assertThat(LifeDecisionLedgerObserver.domainFromScope("casehubio/life/household"))
                .isEqualTo(LifeDomain.HOUSEHOLD);
        assertThat(LifeDecisionLedgerObserver.domainFromScope("casehubio/life/elder_care"))
                .isEqualTo(LifeDomain.ELDER_CARE);
    }

    @Test
    void returnsNullForUnrecognisedScope() {
        assertThat(LifeDecisionLedgerObserver.domainFromScope("unknown")).isNull();
        assertThat(LifeDecisionLedgerObserver.domainFromScope(null)).isNull();
        assertThat(LifeDecisionLedgerObserver.domainFromScope("")).isNull();
    }
}
