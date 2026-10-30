package io.casehub.life.app.repository;

import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.app.entity.JpaLifeCalendarMapping;
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
public class JpaLifeCalendarMappingStore implements LifeCalendarMappingStore {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public LifeCalendarMapping save(LifeCalendarMapping mapping) {
        JpaLifeCalendarMapping entity = em.find(JpaLifeCalendarMapping.class, mapping.id());
        if (entity == null) {
            entity = new JpaLifeCalendarMapping();
            entity.id = mapping.id();
            entity.entityType = mapping.entityType();
            entity.entityId = mapping.entityId();
            entity.calendarPlatformId = mapping.calendarPlatformId();
            entity.calendarId = mapping.calendarId();
            entity.calendarEventId = mapping.calendarEventId();
            entity.domain = mapping.domain();
            entity.summary = mapping.summary();
            entity.createdAt = mapping.createdAt();
            entity.updatedAt = mapping.updatedAt();
            em.persist(entity);
        } else {
            entity.calendarEventId = mapping.calendarEventId();
            entity.summary = mapping.summary();
            entity.updatedAt = mapping.updatedAt();
        }
        return toRecord(entity);
    }

    @Override
    public Optional<LifeCalendarMapping> findByEntity(CalendarEntityType entityType, String entityId) {
        return em.createQuery(
                        "SELECT m FROM JpaLifeCalendarMapping m WHERE m.entityType = :type AND m.entityId = :eid",
                        JpaLifeCalendarMapping.class)
                .setParameter("type", entityType)
                .setParameter("eid", entityId)
                .getResultStream().findFirst().map(this::toRecord);
    }

    @Override
    public List<LifeCalendarMapping> findByCalendarEventId(String calendarEventId) {
        return em.createQuery(
                        "SELECT m FROM JpaLifeCalendarMapping m WHERE m.calendarEventId = :ceid",
                        JpaLifeCalendarMapping.class)
                .setParameter("ceid", calendarEventId)
                .getResultList().stream().map(this::toRecord).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        JpaLifeCalendarMapping entity = em.find(JpaLifeCalendarMapping.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    private LifeCalendarMapping toRecord(JpaLifeCalendarMapping e) {
        return new LifeCalendarMapping(
                e.id, e.entityType, e.entityId,
                e.calendarPlatformId, e.calendarId, e.calendarEventId,
                e.domain, e.summary, e.createdAt, e.updatedAt);
    }
}
