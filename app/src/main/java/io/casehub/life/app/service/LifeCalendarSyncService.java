package io.casehub.life.app.service;

import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.model.EventDetails;
import io.casehub.connectors.calendar.spi.CalendarPlatform;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeCalendarMapping;
import io.casehub.life.api.LifeCalendarMappingStore;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.response.LifeCalendarEventResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LifeCalendarSyncService {

    private static final Logger LOG = Logger.getLogger(LifeCalendarSyncService.class);

    private final CalendarPlatformService platformService;
    private final LifeCalendarMappingStore mappingStore;
    private final String defaultPlatformId;
    private final String defaultCalendarId;

    @Inject
    public LifeCalendarSyncService(
            CalendarPlatformService platformService,
            LifeCalendarMappingStore mappingStore,
            @ConfigProperty(name = "casehub.life.calendar.default-platform-id", defaultValue = "google") String defaultPlatformId,
            @ConfigProperty(name = "casehub.life.calendar.default-calendar-id", defaultValue = "primary") String defaultCalendarId) {
        this.platformService = platformService;
        this.mappingStore = mappingStore;
        this.defaultPlatformId = defaultPlatformId;
        this.defaultCalendarId = defaultCalendarId;
    }

    public void syncWorkItem(UUID workItemId, String title, LifeDomain domain, Instant expiresAt) {
        var existing = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString());
        var platform = platformService.platform(defaultPlatformId);
        var summary = "[" + domain.name() + "] " + title;
        var description = "casehub:domain:" + domain.name();
        var timing = buildTiming(expiresAt);
        var details = new EventDetails(summary, description, null, timing, List.of());

        if (existing.isPresent()) {
            var mapping = existing.get();
            platform.updateEvent(mapping.calendarId(), mapping.calendarEventId(), details);
            mappingStore.save(new LifeCalendarMapping(
                    mapping.id(), mapping.entityType(), mapping.entityId(),
                    mapping.calendarPlatformId(), mapping.calendarId(), mapping.calendarEventId(),
                    domain, summary, mapping.createdAt(), Instant.now()));
        } else {
            var created = platform.createEvent(defaultCalendarId, details);
            mappingStore.save(new LifeCalendarMapping(
                    UUID.randomUUID(), CalendarEntityType.WORK_ITEM, workItemId.toString(),
                    defaultPlatformId, defaultCalendarId, created.id(),
                    domain, summary, Instant.now(), Instant.now()));
        }
    }

    public void removeWorkItem(UUID workItemId) {
        mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString())
                .ifPresent(mapping -> {
                    try {
                        var platform = platformService.platform(mapping.calendarPlatformId());
                        platform.deleteEvent(mapping.calendarId(), mapping.calendarEventId());
                    } catch (Exception e) {
                        LOG.warnf(e, "Failed to delete calendar event %s — orphaned event may remain", mapping.calendarEventId());
                    }
                    mappingStore.delete(mapping.id());
                });
    }

    public List<LifeCalendarEventResponse> listEvents(Instant from, Instant to) {
        CalendarPlatform platform;
        try {
            platform = platformService.platform(defaultPlatformId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        var events = platform.listEvents(defaultCalendarId, from, to);
        return events.stream().map(this::toResponse).toList();
    }

    private LifeCalendarEventResponse toResponse(CalendarEvent event) {
        var mappings = mappingStore.findByCalendarEventId(event.id());
        if (!mappings.isEmpty()) {
            var m = mappings.getFirst();
            return new LifeCalendarEventResponse(
                    event.id(), event.calendarId(), event.summary(), event.description(),
                    event.location(), event.timing(), event.attendees(),
                    m.domain(), m.entityType(), m.entityId(), true);
        }
        LifeDomain inferred = inferDomainFromDescription(event.description());
        return new LifeCalendarEventResponse(
                event.id(), event.calendarId(), event.summary(), event.description(),
                event.location(), event.timing(), event.attendees(),
                inferred, null, null, false);
    }

    static LifeDomain inferDomainFromDescription(String description) {
        if (description == null) return null;
        if (description.contains("casehub:domain:")) {
            var domainStr = description.substring(description.indexOf("casehub:domain:") + 15);
            if (domainStr.contains("\n")) domainStr = domainStr.substring(0, domainStr.indexOf('\n'));
            try {
                return LifeDomain.valueOf(domainStr.trim());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static EventTiming buildTiming(Instant expiresAt) {
        LocalDate date = LocalDate.ofInstant(expiresAt, ZoneOffset.UTC);
        return new EventTiming.AllDay(date, date.plusDays(1));
    }
}
