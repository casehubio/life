package io.casehub.life.app.engine.agent;

import java.util.List;

public record SearchProductsResult(int resultCount, String recommendedProductId,
                                   String recommendedProductName, int recommendedPrice,
                                   String reason, List<String> toolsUsed) {}
