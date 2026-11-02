package io.casehub.life.app.engine.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeAvatarDescriptorProviderTest {

    @Test
    void descriptorHasAvatarIdentity() {
        var provider = new LifeAvatarDescriptorProvider(
            "Life", "278776f9-e1b0-46fb-9032-8bddebdcf9ce", "GB");

        var descriptor = provider.getDescriptor();
        assertEquals("Life", descriptor.name());
        assertNotNull(descriptor.agentId());
        assertTrue(descriptor.agentId().contains("avatar"));
    }

    @Test
    void briefingContainsHouseholdContext() {
        var provider = new LifeAvatarDescriptorProvider(
            "Life", "278776f9-e1b0-46fb-9032-8bddebdcf9ce", "GB");

        var descriptor = provider.getDescriptor();
        String briefing = descriptor.briefing();
        assertNotNull(briefing);
        assertTrue(briefing.contains("household"));
    }

    @Test
    void tenancyIdIsPassedThrough() {
        var provider = new LifeAvatarDescriptorProvider(
            "Life", "test-tenant-id", "US");

        var descriptor = provider.getDescriptor();
        assertEquals("test-tenant-id", descriptor.tenancyId());
        assertEquals("US", descriptor.jurisdiction());
    }
}
