package io.casehub.life.app.service;

import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.EventDetails;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarConflictStore;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.app.event.CalendarConflictResolved;
import io.casehub.work.runtime.model.WorkItemEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LifeCalendarConflictService {

    private final LifeCalendarConflictStore conflictStore;
    private final LifeCalendarMappingStore mappingStore;
    private final CalendarPlatformService platformService;
    private final EntityManager em;
    private final Event<CalendarConflictResolved> resolvedEvent;

    @Inject
    public LifeCalendarConflictService(
            LifeCalendarConflictStore conflictStore,
            LifeCalendarMappingStore mappingStore,
            CalendarPlatformService platformService,
            EntityManager em,
            Event<CalendarConflictResolved> resolvedEvent) {
        this.conflictStore = conflictStore;
        this.mappingStore = mappingStore;
        this.platformService = platformService;
        this.em = em;
        this.resolvedEvent = resolvedEvent;
    }

    @Transactional
    public void accept(UUID conflictId) {
        var conflict = loadAndValidate(conflictId);
        var mapping = mappingStore.findById(conflict.mappingId()).orElse(null);

        if (conflict.conflictType() == CalendarConflictType.MODIFIED) {
            acceptModified(conflict, mapping);
        } else {
            acceptDeleted(conflict);
        }

        resolveConflict(conflict, CalendarConflictStatus.ACCEPTED, "life-system", mapping);
    }

    @Transactional
    public void dismiss(UUID conflictId) {
        var conflict = loadAndValidate(conflictId);
        var mapping = mappingStore.findById(conflict.mappingId()).orElse(null);

        if (conflict.conflictType() == CalendarConflictType.MODIFIED) {
            dismissModified(conflict, mapping);
        } else {
            dismissDeleted(conflict, mapping);
        }

        resolveConflict(conflict, CalendarConflictStatus.DISMISSED, "life-system", mapping);
    }

    private void acceptModified(LifeCalendarConflict conflict, LifeCalendarMapping mapping) {
        if (mapping == null) return;

        if (conflict.externalTiming() != null) {
            var workItemId = UUID.fromString(mapping.entityId());
            var workItem = em.find(WorkItemEntity.class, workItemId);
            if (workItem != null) {
                workItem.expiresAt = timingToInstant(conflict.externalTiming());
            }
        }

        mappingStore.save(new LifeCalendarMapping(
                mapping.id(), mapping.entityType(), mapping.entityId(),
                mapping.calendarPlatformId(), mapping.calendarId(), mapping.calendarEventId(),
                mapping.domain(), conflict.externalSummary() != null ? conflict.externalSummary() : mapping.summary(),
                conflict.externalTiming() != null ? conflict.externalTiming() : mapping.pushedTiming(),
                mapping.createdAt(), Instant.now()));
    }

    private void acceptDeleted(LifeCalendarConflict conflict) {
        conflictStore.deleteByMapping(conflict.mappingId());
        mappingStore.delete(conflict.mappingId());
    }

    private void dismissModified(LifeCalendarConflict conflict, LifeCalendarMapping mapping) {
        if (mapping == null) return;
        var platform = platformService.platform(mapping.calendarPlatformId());
        var details = new EventDetails(
                mapping.summary(), "casehub:domain:" + mapping.domain().name(),
                null, mapping.pushedTiming(), List.of());
        platform.updateEvent(mapping.calendarId(), mapping.calendarEventId(), details);
    }

    private void dismissDeleted(LifeCalendarConflict conflict, LifeCalendarMapping mapping) {
        if (mapping == null) return;
        var platform = platformService.platform(mapping.calendarPlatformId());
        var details = new EventDetails(
                mapping.summary(), "casehub:domain:" + mapping.domain().name(),
                null, mapping.pushedTiming(), List.of());
        var created = platform.createEvent(mapping.calendarId(), details);

        mappingStore.save(new LifeCalendarMapping(
                mapping.id(), mapping.entityType(), mapping.entityId(),
                mapping.calendarPlatformId(), mapping.calendarId(), created.id(),
                mapping.domain(), mapping.summary(), mapping.pushedTiming(),
                mapping.createdAt(), Instant.now()));
    }

    private LifeCalendarConflict loadAndValidate(UUID conflictId) {
        var conflict = conflictStore.findById(conflictId)
                .orElseThrow(() -> new WebApplicationException("Conflict not found", Response.Status.NOT_FOUND));
        if (conflict.status() != CalendarConflictStatus.DETECTED) {
            throw new WebApplicationException("Conflict already resolved: 409", Response.Status.CONFLICT);
        }
        return conflict;
    }

    private void resolveConflict(LifeCalendarConflict conflict, CalendarConflictStatus resolution,
                                  String resolvedBy, LifeCalendarMapping mapping) {
        var resolved = new LifeCalendarConflict(
                conflict.id(), conflict.mappingId(), conflict.conflictType(), conflict.detectedAt(),
                conflict.externalSummary(), conflict.externalTiming(), resolution,
                Instant.now(), resolvedBy, conflict.domain());
        conflictStore.save(resolved);

        String entityId = mapping != null ? mapping.entityId() : "";
        resolvedEvent.fire(new CalendarConflictResolved(
                conflict.id(), resolution, resolvedBy, conflict.domain(), entityId));
    }

    static Instant timingToInstant(EventTiming timing) {
        return switch (timing) {
            case EventTiming.AllDay allDay -> allDay.start().atStartOfDay(ZoneOffset.UTC).toInstant();
            case EventTiming.Timed timed -> timed.start();
        };
    }
}
