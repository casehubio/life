package io.casehub.life.app.engine.agent;

import java.util.List;

public record FulfilOrderResult(String orderId, String orderStatus, int totalAmount,
                                String notificationMessageId, List<String> toolsUsed) {}
