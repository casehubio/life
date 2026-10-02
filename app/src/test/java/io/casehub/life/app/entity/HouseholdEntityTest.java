package io.casehub.life.app.entity;

import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.MemberRelationship;
import io.casehub.life.app.LifeTestFixtures;
import io.casehub.platform.testing.FixedCurrentPrincipal;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@TestSecurity(user = "admin", roles = {"household-admin"})
class HouseholdEntityTest {

    @Inject FixedCurrentPrincipal fixedPrincipal;

    @BeforeEach
    @Transactional
    void setup() {
        fixedPrincipal.setGroups(Set.of(HouseholdGroups.ADMIN));
        HouseholdMember.deleteAll();
        Household.deleteAll();
        LifeTestFixtures.seedStandardTemplates();
    }

    @Test
    @Transactional
    void createHousehold_persistsWithDefaults() {
        Household h = new Household();
        h.id = UUID.fromString("278776f9-e1b0-46fb-9032-8bddebdcf9ce");
        h.name = "Test Family";
        h.persist();

        assertThat(h.createdAt).isNotNull();
        assertThat(h.timezone).isEqualTo("UTC");
        assertThat(Household.findByTenancyId(h.id)).isPresent();
    }

    @Test
    @Transactional
    void createMember_validatesRole() {
        Household h = new Household();
        h.id = UUID.fromString("278776f9-e1b0-46fb-9032-8bddebdcf9ce");
        h.name = "Test";
        h.persist();

        HouseholdMember m = new HouseholdMember();
        m.householdId = h.id;
        m.keycloakUserId = "kc-001";
        m.name = "Mark";
        m.role = "invalid-role";
        assertThatThrownBy(m::persist).hasMessageContaining("Invalid role");
    }

    @Test
    @Transactional
    void createMember_validRole_persists() {
        Household h = new Household();
        h.id = UUID.fromString("278776f9-e1b0-46fb-9032-8bddebdcf9ce");
        h.name = "Test";
        h.persist();

        HouseholdMember m = new HouseholdMember();
        m.householdId = h.id;
        m.keycloakUserId = "kc-001";
        m.name = "Mark";
        m.role = HouseholdGroups.ADMIN;
        m.relationship = MemberRelationship.PARENT;
        m.persist();

        List<HouseholdMember> members = HouseholdMember.findByHouseholdId(h.id);
        assertThat(members).hasSize(1);
        assertThat(members.get(0).name).isEqualTo("Mark");
    }
}
