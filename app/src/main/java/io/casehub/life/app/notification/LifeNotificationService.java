package io.casehub.life.app.notification;

import io.casehub.life.api.LifeDomain;
import io.casehub.platform.api.delivery.DeliveryChannelRegistry;
import io.casehub.platform.api.delivery.NotificationDeliverer;
import io.casehub.platform.api.notification.NotificationInput;
import io.casehub.platform.api.notification.NotificationSeverity;
import io.casehub.platform.api.notification.NotificationSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class LifeNotificationService {

    private static final Logger LOG = Logger.getLogger(LifeNotificationService.class);

    @Inject
    DeliveryChannelRegistry channelRegistry;

    @Inject
    LifeNotificationRouteConfig routeConfig;

    public void notifySlaBreachEscalation(LifeDomain domain, String taskTitle, UUID workItemId) {
        var input = new NotificationInput(
                routeConfig.defaultRecipient(),
                routeConfig.tenancyId(),
                "SLA Breach: " + taskTitle,
                "Task '%s' in %s has breached its SLA deadline and been escalated.".formatted(
                        taskTitle, domain.name().toLowerCase().replace('_', ' ')),
                "sla.breached",
                NotificationSeverity.URGENT,
                "/life-tasks/" + workItemId,
                new NotificationSource(
                        UUID.randomUUID().toString(), "work-item",
                        workItemId.toString(), "life-system"));
        deliverToConfiguredChannels(domain, input);
    }

    public void notifyWatchdogEscalation(LifeDomain domain, String escalationTitle) {
        var input = new NotificationInput(
                routeConfig.defaultRecipient(),
                routeConfig.tenancyId(),
                "Watchdog Alert: " + escalationTitle,
                "A commitment in %s has expired without response and been escalated.".formatted(
                        domain.name().toLowerCase().replace('_', ' ')),
                "watchdog.escalated",
                NotificationSeverity.WARNING,
                null,
                new NotificationSource(
                        UUID.randomUUID().toString(), "commitment",
                        UUID.randomUUID().toString(), "life-system"));
        deliverToConfiguredChannels(domain, input);
    }

    private void deliverToConfiguredChannels(LifeDomain domain, NotificationInput input) {
        for (String channelId : routeConfig.channels()) {
            channelRegistry.resolveDeliverer(channelId).ifPresentOrElse(
                    deliverer -> deliver(deliverer, input),
                    () -> LOG.debugf("No deliverer registered for channel '%s'", channelId));
        }
    }

    private void deliver(NotificationDeliverer deliverer, NotificationInput input) {
        try {
            var result = deliverer.deliver(input);
            if (!result.success()) {
                LOG.warnf("Notification delivery failed on channel '%s': %s",
                        deliverer.channelId(), result.failureReason());
            }
        } catch (Exception e) {
            LOG.warnf(e, "Notification delivery error on channel '%s'", deliverer.channelId());
        }
    }
}
