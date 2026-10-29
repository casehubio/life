package io.casehub.life.api.coordination;

import java.util.List;
import java.util.Map;

public record WorkerActivation(
    List<String> skipWorkers,
    Map<String, Map<String, Object>> workerParams) {}
