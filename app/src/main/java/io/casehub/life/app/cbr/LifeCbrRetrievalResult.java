package io.casehub.life.app.cbr;

import io.casehub.life.api.CbrSuggestions;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrMatch;

import java.util.List;
import java.util.Map;

public record LifeCbrRetrievalResult(
        CbrSuggestions suggestions,
        List<CbrMatch<CbrPlanRecord>> cases,
        Map<String, FeatureValue> currentFeatures) {

    public static final LifeCbrRetrievalResult EMPTY =
            new LifeCbrRetrievalResult(CbrSuggestions.EMPTY, List.of(), Map.of());
}
