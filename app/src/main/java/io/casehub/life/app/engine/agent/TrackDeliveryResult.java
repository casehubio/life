package io.casehub.life.app.engine.agent;

import java.util.List;

public record TrackDeliveryResult(boolean delivered, String orderId, String trackingStatus,
                                  String estimatedDelivery,
                                  String notificationMessageId, List<String> toolsUsed) {}
