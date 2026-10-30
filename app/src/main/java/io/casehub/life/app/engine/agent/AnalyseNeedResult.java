package io.casehub.life.app.engine.agent;

import java.util.List;

public record AnalyseNeedResult(String category, String itemType, String intent,
                                int estimatedBudget, int quantity, String urgency,
                                String recipientName, String occasion,
                                List<String> toolsUsed) {}
