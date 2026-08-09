package io.casehub.life.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.life.api.response.CbrPrecedentResponse;
import io.casehub.life.app.entity.LifeCaseTracker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LifeCbrQueryService {

    private static final Logger LOG = Logger.getLogger(LifeCbrQueryService.class);
    private static final TypeReference<List<CbrPrecedentResponse>> PRECEDENT_LIST_TYPE =
            new TypeReference<>() {};

    @Inject ObjectMapper objectMapper;

    @Transactional
    public Optional<List<CbrPrecedentResponse>> findPrecedentsByCase(UUID caseTrackerId) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(caseTrackerId);
        if (tracker == null) return Optional.empty();
        if (tracker.cbrPrecedentsJson == null) return Optional.of(List.of());

        try {
            List<CbrPrecedentResponse> precedents =
                    objectMapper.readValue(tracker.cbrPrecedentsJson, PRECEDENT_LIST_TYPE);
            return Optional.of(precedents);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to deserialize CBR precedents for case %s", caseTrackerId);
            return Optional.of(List.of());
        }
    }
}
