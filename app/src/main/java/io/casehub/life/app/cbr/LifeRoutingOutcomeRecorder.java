package io.casehub.life.app.cbr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.spi.routing.AgentRoutingContext;
import io.casehub.api.spi.routing.RoutingOutcome;
import io.casehub.api.spi.routing.RoutingOutcomeRecorder;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.platform.api.path.Path;
import io.casehub.neocortex.memory.cbr.CbrCaseMemoryStore;
import io.casehub.neocortex.memory.cbr.PlanCbrCase;
import io.casehub.neocortex.memory.cbr.PlanTrace;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LifeRoutingOutcomeRecorder implements RoutingOutcomeRecorder {

    private static final Logger LOG = Logger.getLogger(LifeRoutingOutcomeRecorder.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CbrCaseMemoryStore cbrStore;
    private final LifeCbrFeatureExtractor featureExtractor;
    private final CaseTypeLookup caseTypeLookup;
    private final Map<String, LifeCbrDescriptionProvider> providers;

    @Inject
    public LifeRoutingOutcomeRecorder(CbrCaseMemoryStore cbrStore,
                                       LifeCbrFeatureExtractor featureExtractor,
                                       CaseTypeLookup caseTypeLookup,
                                       Instance<LifeCbrDescriptionProvider> providers) {
        this.cbrStore = cbrStore;
        this.featureExtractor = featureExtractor;
        this.caseTypeLookup = caseTypeLookup;
        this.providers = new HashMap<>();
        providers.stream().forEach(p -> this.providers.put(p.caseType(), p));
    }

    @Override
    public void record(AgentRoutingContext context, String workerId, String bindingName,
                       RoutingOutcome outcome, @Nullable Duration executionDuration) {
        try {
            Optional<String> caseTypeOpt = caseTypeLookup.findCaseType(context.caseId());
            if (caseTypeOpt.isEmpty()) { return; }
            String caseType = caseTypeOpt.get();

            LifeCbrDescriptionProvider descProvider = providers.get(caseType);
            if (descProvider == null) { return; }

            var extraction = featureExtractor.extract(caseType, context.caseContext());
            if (extraction.isEmpty()) { return; }

            var                 result   = extraction.get();
            Map<String, Object> caseData = MAPPER.convertValue(context.caseContext(), MAP_TYPE);

            PlanTrace trace = new PlanTrace(
                    bindingName, context.capabilityName(),
                    workerId, outcome.name(), 0, Map.of(), null);

            PlanCbrCase cbrCase = new PlanCbrCase(
                    descProvider.describeProblem(caseData),
                    descProvider.describeSolution(caseData),
                    outcome.name(),
                    null,
                    result.features(),
                    List.of(trace),
                    null,
                    null);

            cbrStore.store(
                    cbrCase,
                    caseType,
                    "agent-routing",
                    new MemoryDomain(result.config().domain()),
                    context.tenancyId(),
                    context.caseId().toString(),
                    Path.parse(result.config().domain()));
        } catch (Exception e) {
            LOG.warnf(e, "CBR routing retention failed — proceeding without recording");
        }
    }

    @ApplicationScoped
    public static class CaseTypeLookup {
        public Optional<String> findCaseType(UUID engineCaseId) {
            return LifeCaseTracker.findByEngineCaseId(engineCaseId)
                    .map(t -> t.caseType);
        }
    }
}
