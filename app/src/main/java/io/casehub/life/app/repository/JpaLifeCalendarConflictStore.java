package io.casehub.life.app.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarConflictStore;
import io.casehub.life.app.entity.JpaLifeCalendarConflict;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Alternative
@jakarta.annotation.Priority(1)
@ApplicationScoped
public class JpaLifeCalendarConflictStore implements LifeCalendarConflictStore {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public LifeCalendarConflict save(LifeCalendarConflict conflict) {
        var entity = em.find(JpaLifeCalendarConflict.class, conflict.id());
        if (entity == null) {
            entity = new JpaLifeCalendarConflict();
            entity.id = conflict.id();
        }
        entity.mappingId = conflict.mappingId();
        entity.conflictType = conflict.conflictType();
        entity.detectedAt = conflict.detectedAt();
        entity.externalSummary = conflict.externalSummary();
        entity.externalTiming = serializeTiming(conflict.externalTiming());
        entity.status = conflict.status();
        entity.resolvedAt = conflict.resolvedAt();
        entity.resolvedBy = conflict.resolvedBy();
        entity.domain = conflict.domain();
        em.persist(entity);
        return toRecord(entity);
    }

    @Override
    public Optional<LifeCalendarConflict> findById(UUID id) {
        return Optional.ofNullable(em.find(JpaLifeCalendarConflict.class, id)).map(this::toRecord);
    }

    @Override
    public List<LifeCalendarConflict> findByMapping(UUID mappingId) {
        return em.createQuery(
                        "SELECT c FROM JpaLifeCalendarConflict c WHERE c.mappingId = :mid",
                        JpaLifeCalendarConflict.class)
                .setParameter("mid", mappingId)
                .getResultList().stream().map(this::toRecord).toList();
    }

    @Override
    public List<LifeCalendarConflict> findUnresolved() {
        return em.createQuery(
                        "SELECT c FROM JpaLifeCalendarConflict c WHERE c.status = :status",
                        JpaLifeCalendarConflict.class)
                .setParameter("status", CalendarConflictStatus.DETECTED)
                .getResultList().stream().map(this::toRecord).toList();
    }

    @Override
    public long countUnresolved() {
        return em.createQuery(
                        "SELECT COUNT(c) FROM JpaLifeCalendarConflict c WHERE c.status = :status",
                        Long.class)
                .setParameter("status", CalendarConflictStatus.DETECTED)
                .getSingleResult();
    }

    @Override
    public Optional<LifeCalendarConflict> findUnresolvedByMapping(UUID mappingId) {
        return em.createQuery(
                        "SELECT c FROM JpaLifeCalendarConflict c WHERE c.mappingId = :mid AND c.status = :status",
                        JpaLifeCalendarConflict.class)
                .setParameter("mid", mappingId)
                .setParameter("status", CalendarConflictStatus.DETECTED)
                .getResultStream().findFirst().map(this::toRecord);
    }

    @Override
    @Transactional
    public void deleteByMapping(UUID mappingId) {
        em.createQuery("DELETE FROM JpaLifeCalendarConflict c WHERE c.mappingId = :mid")
                .setParameter("mid", mappingId)
                .executeUpdate();
    }

    private LifeCalendarConflict toRecord(JpaLifeCalendarConflict e) {
        return new LifeCalendarConflict(
                e.id, e.mappingId, e.conflictType, e.detectedAt,
                e.externalSummary, deserializeTiming(e.externalTiming),
                e.status, e.resolvedAt, e.resolvedBy, e.domain);
    }

    private static String serializeTiming(EventTiming timing) {
        if (timing == null) return null;
        try {
            return MAPPER.writeValueAsString(timing);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize EventTiming", e);
        }
    }

    private static EventTiming deserializeTiming(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, EventTiming.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize EventTiming", e);
        }
    }
}
