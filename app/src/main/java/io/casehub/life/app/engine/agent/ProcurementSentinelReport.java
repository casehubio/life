package io.casehub.life.app.engine.agent;

import java.util.List;

public record ProcurementSentinelReport(boolean escalationRequired, String orderId,
                                        String deliveryStatus, String alertReason,
                                        String alertMessageId, List<String> toolsUsed) {}
