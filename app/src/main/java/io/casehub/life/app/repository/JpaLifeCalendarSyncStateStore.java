package io.casehub.life.app.repository;

import io.casehub.life.api.LifeCalendarSyncState;
import io.casehub.life.api.LifeCalendarSyncStateStore;
import io.casehub.life.app.entity.JpaLifeCalendarSyncState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

@ApplicationScoped
public class JpaLifeCalendarSyncStateStore implements LifeCalendarSyncStateStore {

    @Inject
    EntityManager em;

    @Override
    public LifeCalendarSyncState save(LifeCalendarSyncState state) {
        var entity = em.find(JpaLifeCalendarSyncState.class, state.id());
        if (entity == null) {
            entity = new JpaLifeCalendarSyncState();
            entity.id = state.id();
        }
        entity.platformId = state.platformId();
        entity.calendarId = state.calendarId();
        entity.syncToken = state.syncToken();
        entity.lastSyncAt = state.lastSyncAt();
        em.persist(entity);
        return toRecord(entity);
    }

    @Override
    public Optional<LifeCalendarSyncState> findByCalendar(String platformId, String calendarId) {
        try {
            var entity = em.createQuery(
                            "SELECT s FROM JpaLifeCalendarSyncState s WHERE s.platformId = :pid AND s.calendarId = :cid",
                            JpaLifeCalendarSyncState.class)
                    .setParameter("pid", platformId)
                    .setParameter("cid", calendarId)
                    .getSingleResult();
            return Optional.of(toRecord(entity));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    private static LifeCalendarSyncState toRecord(JpaLifeCalendarSyncState e) {
        return new LifeCalendarSyncState(e.id, e.platformId, e.calendarId, e.syncToken, e.lastSyncAt);
    }
}
