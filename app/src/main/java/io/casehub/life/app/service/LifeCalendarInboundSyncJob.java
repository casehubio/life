package io.casehub.life.app.service;

import io.casehub.connectors.SyncRequest;
import io.casehub.connectors.SyncResult;
import io.casehub.connectors.SyncTokenExpiredException;
import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.spi.CalendarPlatform;
import io.casehub.life.api.LifeCalendarSyncState;
import io.casehub.life.api.LifeCalendarSyncStateStore;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@ApplicationScoped
public class LifeCalendarInboundSyncJob {

    private static final Logger LOG = Logger.getLogger(LifeCalendarInboundSyncJob.class);

    private final CalendarPlatformService platformService;
    private final LifeCalendarSyncStateStore syncStateStore;
    private final LifeCalendarConflictDetector conflictDetector;
    private final String defaultPlatformId;
    private final String defaultCalendarId;
    private final int pageSize;

    @Inject
    public LifeCalendarInboundSyncJob(
            CalendarPlatformService platformService,
            LifeCalendarSyncStateStore syncStateStore,
            LifeCalendarConflictDetector conflictDetector,
            @ConfigProperty(name = "casehub.life.calendar.default-platform-id", defaultValue = "google") String defaultPlatformId,
            @ConfigProperty(name = "casehub.life.calendar.default-calendar-id", defaultValue = "primary") String defaultCalendarId,
            @ConfigProperty(name = "casehub.life.calendar.sync.page-size", defaultValue = "100") int pageSize) {
        this.platformService = platformService;
        this.syncStateStore = syncStateStore;
        this.conflictDetector = conflictDetector;
        this.defaultPlatformId = defaultPlatformId;
        this.defaultCalendarId = defaultCalendarId;
        this.pageSize = pageSize;
    }

    @Scheduled(every = "${casehub.life.calendar.sync.interval:PT5M}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledSync() {
        try {
            executeSync();
        } catch (Exception e) {
            LOG.errorf(e, "Calendar inbound sync failed");
        }
    }

    @Transactional
    public int executeSync() {
        CalendarPlatform platform;
        try {
            platform = platformService.platform(defaultPlatformId);
        } catch (IllegalArgumentException e) {
            LOG.debugf("Calendar platform '%s' not available — skipping sync", defaultPlatformId);
            return 0;
        }

        var syncState = syncStateStore.findByCalendar(defaultPlatformId, defaultCalendarId);
        String currentToken = syncState.map(LifeCalendarSyncState::syncToken).orElse(null);
        UUID stateId = syncState.map(LifeCalendarSyncState::id).orElse(UUID.randomUUID());

        try {
            return doSync(platform, stateId, currentToken);
        } catch (SyncTokenExpiredException e) {
            LOG.warnf("Sync token expired for %s/%s — performing initial sync", defaultPlatformId, defaultCalendarId);
            syncStateStore.save(new LifeCalendarSyncState(stateId, defaultPlatformId, defaultCalendarId, null, Instant.now()));
            return doSync(platform, stateId, null);
        }
    }

    private int doSync(CalendarPlatform platform, UUID stateId, String syncToken) {
        var allItems = new ArrayList<CalendarEvent>();
        var allDeleted = new ArrayList<String>();
        String latestToken = syncToken;
        boolean hasMore = true;

        while (hasMore) {
            var request = latestToken != null ? new SyncRequest(latestToken, pageSize) : SyncRequest.initial(pageSize);
            SyncResult<CalendarEvent> result = platform.listEventsSync(defaultCalendarId, request);
            allItems.addAll(result.items());
            allDeleted.addAll(result.deletedIds());
            latestToken = result.syncToken();
            hasMore = result.hasMore();
        }

        int conflictsDetected = conflictDetector.detectConflicts(allItems, allDeleted);

        syncStateStore.save(new LifeCalendarSyncState(stateId, defaultPlatformId, defaultCalendarId, latestToken, Instant.now()));

        LOG.infof("Calendar sync complete: %d events processed, %d deletions, %d conflicts detected",
                allItems.size(), allDeleted.size(), conflictsDetected);

        return conflictsDetected;
    }
}
