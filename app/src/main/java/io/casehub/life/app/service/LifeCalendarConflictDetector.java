package io.casehub.life.app.service;

import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarConflict;
import io.casehub.life.api.LifeCalendarConflictStore;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.app.event.CalendarConflictDetected;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class LifeCalendarConflictDetector {

    private final LifeCalendarMappingStore mappingStore;
    private final LifeCalendarConflictStore conflictStore;
    private final Event<CalendarConflictDetected> conflictDetectedEvent;

    @Inject
    public LifeCalendarConflictDetector(
            LifeCalendarMappingStore mappingStore,
            LifeCalendarConflictStore conflictStore,
            Event<CalendarConflictDetected> conflictDetectedEvent) {
        this.mappingStore = mappingStore;
        this.conflictStore = conflictStore;
        this.conflictDetectedEvent = conflictDetectedEvent;
    }

    public int detectConflicts(List<CalendarEvent> changedEvents, List<String> deletedIds) {
        int count = 0;
        for (var event : changedEvents) {
            if (processChangedEvent(event)) count++;
        }
        for (var deletedId : deletedIds) {
            if (processDeletedEvent(deletedId)) count++;
        }
        return count;
    }

    private boolean processChangedEvent(CalendarEvent event) {
        var mappings = mappingStore.findByCalendarEventId(event.id());
        if (mappings.isEmpty()) return false;

        var mapping = mappings.getFirst();
        if (mapping.entityType() != CalendarEntityType.WORK_ITEM) return false;
        if (mapping.pushedTiming() == null) return false;

        boolean summaryMatch = Objects.equals(event.summary(), mapping.summary());
        boolean timingMatch = timingsEqual(event.timing(), mapping.pushedTiming());
        if (summaryMatch && timingMatch) return false;

        var existing = conflictStore.findUnresolvedByMapping(mapping.id());
        if (existing.isPresent()) {
            var existingConflict = existing.get();
            boolean sameExternal = Objects.equals(event.summary(), existingConflict.externalSummary())
                    && timingsEqual(event.timing(), existingConflict.externalTiming());
            if (sameExternal) return false;

            var updated = new LifeCalendarConflict(
                    existingConflict.id(), existingConflict.mappingId(), existingConflict.conflictType(),
                    existingConflict.detectedAt(), event.summary(), event.timing(),
                    existingConflict.status(), existingConflict.resolvedAt(), existingConflict.resolvedBy(),
                    existingConflict.domain());
            conflictStore.save(updated);
            fireDetected(updated, mapping);
            return true;
        }

        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.MODIFIED, Instant.now(),
                event.summary(), event.timing(), CalendarConflictStatus.DETECTED,
                null, null, mapping.domain());
        conflictStore.save(conflict);
        fireDetected(conflict, mapping);
        return true;
    }

    private boolean processDeletedEvent(String deletedEventId) {
        var mappings = mappingStore.findByCalendarEventId(deletedEventId);
        if (mappings.isEmpty()) return false;

        var mapping = mappings.getFirst();
        if (mapping.entityType() != CalendarEntityType.WORK_ITEM) return false;

        var existing = conflictStore.findUnresolvedByMapping(mapping.id());
        if (existing.isPresent()) return false;

        var conflict = new LifeCalendarConflict(
                UUID.randomUUID(), mapping.id(), CalendarConflictType.DELETED, Instant.now(),
                null, null, CalendarConflictStatus.DETECTED,
                null, null, mapping.domain());
        conflictStore.save(conflict);
        fireDetected(conflict, mapping);
        return true;
    }

    private void fireDetected(LifeCalendarConflict conflict, LifeCalendarMapping mapping) {
        conflictDetectedEvent.fire(new CalendarConflictDetected(
                conflict.id(), conflict.conflictType(), conflict.domain(),
                mapping.entityId(), mapping.id()));
    }

    private static boolean timingsEqual(EventTiming a, EventTiming b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
