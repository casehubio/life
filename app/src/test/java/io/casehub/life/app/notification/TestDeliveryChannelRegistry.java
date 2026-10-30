package io.casehub.life.app.notification;

import io.casehub.platform.api.delivery.DeliveryChannelDescriptor;
import io.casehub.platform.api.delivery.DeliveryChannelRegistry;
import io.casehub.platform.api.delivery.DeliveryResult;
import io.casehub.platform.api.delivery.NotificationDeliverer;
import io.casehub.platform.api.notification.NotificationInput;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Alternative
@Priority(10)
@ApplicationScoped
public class TestDeliveryChannelRegistry implements DeliveryChannelRegistry {

    private final CopyOnWriteArrayList<NotificationInput> deliveredNotifications = new CopyOnWriteArrayList<>();

    private final NotificationDeliverer testDeliverer = new NotificationDeliverer() {
        @Override
        public String channelId() {
            return "sms";
        }

        @Override
        public DeliveryResult deliver(NotificationInput notification) {
            deliveredNotifications.add(notification);
            return new DeliveryResult(true, null);
        }
    };

    @Override
    public void register(DeliveryChannelDescriptor descriptor, NotificationDeliverer deliverer) {
    }

    @Override
    public Optional<DeliveryChannelDescriptor> resolve(String channelId) {
        return Optional.empty();
    }

    @Override
    public Optional<NotificationDeliverer> resolveDeliverer(String channelId) {
        if ("sms".equals(channelId)) {
            return Optional.of(testDeliverer);
        }
        return Optional.empty();
    }

    @Override
    public Set<DeliveryChannelDescriptor> discover() {
        return Set.of();
    }

    public CopyOnWriteArrayList<NotificationInput> deliveredNotifications() {
        return deliveredNotifications;
    }

    public void clear() {
        deliveredNotifications.clear();
    }
}
