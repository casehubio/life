package io.casehub.life.app.service;

import io.casehub.ledger.runtime.service.TrustGateService;
import io.casehub.life.api.LifeActorIds;
import io.casehub.life.api.LifeActorType;
import io.casehub.life.api.request.CreateExternalActorRequest;
import io.casehub.life.api.request.UpdateExternalActorRequest;
import io.casehub.life.api.response.ExternalActorResponse;
import io.casehub.life.api.response.LifeTaskContextResponse;
import io.casehub.life.app.entity.ExternalActor;
import io.casehub.life.app.entity.LifeTaskContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import io.casehub.life.api.response.PagedResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExternalActorService {

    @Inject
    TrustGateService trustGateService;

    @Inject
    EntityManager em;

    @Transactional
    public ExternalActorResponse create(final CreateExternalActorRequest req) {
        final ExternalActor actor = new ExternalActor();
        actor.name = req.name();
        actor.actorType = req.actorType();
        actor.contactMethod = req.contactMethod();
        actor.contactValue = req.contactValue();
        em.persist(actor);
        return toResponse(actor);
    }

    public Optional<ExternalActorResponse> findById(final UUID id) {
        return Optional.ofNullable(em.find(ExternalActor.class, id)).map(this::toResponse);
    }

    @Transactional
    public PagedResponse<ExternalActorResponse> search(
            final String name, final LifeActorType actorType, final String contactMethod,
            final boolean erasedOnly, int page, int size) {
        page = Math.max(page, 0);
        size = Math.max(Math.min(size, 100), 1);
        var params     = new HashMap<String, Object>();
        var conditions = new ArrayList<String>();

        if (name != null && !name.isBlank()) {
            conditions.add("LOWER(name) LIKE LOWER(:name)");
            params.put("name", "%" + name + "%");
        }
        if (actorType != null) {
            conditions.add("actorType = :actorType");
            params.put("actorType", actorType);
        }
        if (contactMethod != null && !contactMethod.isBlank()) {
            conditions.add("contactMethod = :contactMethod");
            params.put("contactMethod", contactMethod);
        }
        if (erasedOnly) {
            conditions.add("gdprErasedAt IS NOT NULL");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(e) FROM ExternalActor e" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        TypedQuery<ExternalActor> selectQuery = em.createQuery("SELECT e FROM ExternalActor e" + where, ExternalActor.class);
        params.forEach(selectQuery::setParameter);
        List<ExternalActor> actors = selectQuery.setFirstResult(page * size).setMaxResults(size).getResultList();
        List<ExternalActorResponse> items  = actors.stream().map(this::toResponse).toList();
        return new PagedResponse<>(items, page, size, total);
    }

    @Transactional
    public Optional<ExternalActorResponse> update(final UUID id, final UpdateExternalActorRequest req) {
        return Optional.ofNullable(em.find(ExternalActor.class, id)).map(existing -> {
            existing.name = req.name();
            existing.actorType = req.actorType();
            existing.contactMethod = req.contactMethod();
            existing.contactValue = req.contactValue();
            return toResponse(existing);
        });
    }

    @Transactional
    public void delete(final UUID id) {
        final ExternalActor actor = Optional.ofNullable(em.find(ExternalActor.class, id))
                .orElseThrow(NotFoundException::new);
        final long referencingTasks = em.createNamedQuery("LifeTaskContext.countByExternalActorId", Long.class)
                .setParameter("externalActorId", id).getSingleResult();
        if (referencingTasks > 0) {
            throw new ClientErrorException(
                    "ExternalActor is referenced by " + referencingTasks + " task(s)",
                    Response.Status.CONFLICT);
        }
        em.remove(actor);
    }


    public List<LifeTaskContextResponse> listTasks(final UUID actorId) {
        return em.createNamedQuery("LifeTaskContext.findByExternalActorId", LifeTaskContext.class)
                .setParameter("externalActorId", actorId)
                .getResultList().stream()
                .map(c -> new LifeTaskContextResponse(c.workItemId, c.domain, c.externalActorId, c.recurrence, c.jurisdiction))
                .toList();
    }

    private ExternalActorResponse toResponse(final ExternalActor actor) {
        ExternalActorResponse.TrustProfile profile;
        if (actor.gdprErasedAt != null) {
            profile = ExternalActorResponse.TrustProfile.EMPTY;
        } else {
            String actorId = LifeActorIds.of(actor.id);
            java.util.OptionalDouble globalOpt = trustGateService.currentScore(actorId);
            Double global = globalOpt.isPresent() ? globalOpt.getAsDouble() : null;
            Map<String, Double> dimensions = trustGateService.allDimensionScores(actorId);
            Map<String, Double> capabilities = trustGateService.allCapabilityScores(actorId);
            profile = new ExternalActorResponse.TrustProfile(global, dimensions, capabilities);
        }
        return new ExternalActorResponse(
                actor.id,
                actor.name,
                actor.actorType,
                actor.contactMethod,
                actor.contactValue,
                actor.createdAt,
                actor.gdprErasedAt,
                profile
        );
    }
}
