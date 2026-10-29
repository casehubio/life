package io.casehub.life.api.coordination;

import java.util.List;
import java.util.Map;

public record CoordinationPlan(
    String preTemplate,
    Map<String, Object> preParams,
    String postTemplate,
    Map<String, Object> postParams,
    List<GeneratedTemplate> generatedTemplates,
    WorkerActivation workerActivation) {}
