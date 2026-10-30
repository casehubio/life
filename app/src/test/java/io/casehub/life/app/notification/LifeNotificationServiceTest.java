package io.casehub.life.app.notification;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.LifeTestFixtures;
import io.casehub.platform.api.notification.NotificationSeverity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestSecurity(user = "household-admin", roles = {"household-admin"})
class LifeNotificationServiceTest {

    @Inject
    LifeNotificationService notificationService;

    @Inject
    TestDeliveryChannelRegistry testRegistry;

    @BeforeEach
    @Transactional
    void setup() {
        testRegistry.clear();
        LifeTestFixtures.seedStandardTemplates();
    }

    @Test
    void slaBreachEscalation_deliversSmsNotification() {
        UUID workItemId = UUID.randomUUID();
        notificationService.notifySlaBreachEscalation(
                LifeDomain.HEALTH, "Overdue GP follow-up", workItemId);

        assertFalse(testRegistry.deliveredNotifications().isEmpty());
        var notification = testRegistry.deliveredNotifications().getFirst();
        assertEquals("household-admin", notification.userId());
        assertTrue(notification.title().contains("SLA Breach"));
        assertTrue(notification.title().contains("Overdue GP follow-up"));
        assertEquals("sla.breached", notification.category());
        assertEquals(NotificationSeverity.URGENT, notification.severity());
        assertTrue(notification.actionUrl().contains(workItemId.toString()));
    }

    @Test
    void watchdogEscalation_deliversWarningNotification() {
        notificationService.notifyWatchdogEscalation(
                LifeDomain.CONTRACTOR_COORDINATION, "Contractor quote overdue");

        assertFalse(testRegistry.deliveredNotifications().isEmpty());
        var notification = testRegistry.deliveredNotifications().getFirst();
        assertTrue(notification.title().contains("Watchdog Alert"));
        assertEquals("watchdog.escalated", notification.category());
        assertEquals(NotificationSeverity.WARNING, notification.severity());
    }
}
