package io.casehub.life.app.observer;

import io.casehub.connectors.calendar.CalendarPlatformService;
import io.casehub.connectors.calendar.model.CalendarEvent;
import io.casehub.connectors.calendar.spi.CalendarPlatform;
import io.casehub.connectors.calendar.spi.EventTiming;
import io.casehub.life.api.CalendarEntityType;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.service.InMemoryLifeCalendarMappingStore;
import io.casehub.life.app.service.LifeCalendarSyncService;
import io.casehub.work.api.WorkItem;
import io.casehub.work.api.WorkItemLifecycleEvent;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.runtime.model.WorkItemEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifeCalendarOutboundObserverTest {

    EntityManager em;
    CalendarPlatform calendarPlatform;
    InMemoryLifeCalendarMappingStore mappingStore;
    LifeCalendarOutboundObserver observer;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        calendarPlatform = mock(CalendarPlatform.class);
        var platformService = mock(CalendarPlatformService.class);
        when(platformService.platform("google")).thenReturn(calendarPlatform);
        mappingStore = new InMemoryLifeCalendarMappingStore();
        var syncService = new LifeCalendarSyncService(platformService, mappingStore, "google", "primary");
        observer = new LifeCalendarOutboundObserver();
        observer.em = em;
        observer.syncService = syncService;
    }

    @Test
    void createdWorkItemWithExpiresAtCreatesCalendarEvent() {
        var workItemId = UUID.randomUUID();
        var workItemEntity = new WorkItemEntity();
        workItemEntity.id = workItemId;
        workItemEntity.title = "GP check-up";
        workItemEntity.scope = "casehubio/life/HEALTH";
        workItemEntity.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        when(em.find(WorkItemEntity.class, workItemId)).thenReturn(workItemEntity);
        var created = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(created);

        var workItem = WorkItem.builder().id(workItemId).status(WorkItemStatus.PENDING).build();
        observer.onWorkItemEvent(WorkItemLifecycleEvent.of("CREATED", workItem, "life-system", null));

        var mapping = mappingStore.findByEntity(CalendarEntityType.WORK_ITEM, workItemId.toString());
        assertThat(mapping).isPresent();
    }

    @Test
    void createdWorkItemWithoutExpiresAtIsIgnored() {
        var workItemId = UUID.randomUUID();
        var workItemEntity = new WorkItemEntity();
        workItemEntity.id = workItemId;
        workItemEntity.title = "Routine task";
        workItemEntity.scope = "casehubio/life/HOUSEHOLD";
        workItemEntity.expiresAt = null;

        when(em.find(WorkItemEntity.class, workItemId)).thenReturn(workItemEntity);

        var workItem = WorkItem.builder().id(workItemId).status(WorkItemStatus.PENDING).build();
        observer.onWorkItemEvent(WorkItemLifecycleEvent.of("CREATED", workItem, "life-system", null));

        verify(calendarPlatform, never()).createEvent(any(), any());
    }

    @Test
    void completedWorkItemDeletesCalendarEvent() {
        var workItemId = UUID.randomUUID();
        var workItemEntity = new WorkItemEntity();
        workItemEntity.id = workItemId;
        workItemEntity.title = "GP check-up";
        workItemEntity.scope = "casehubio/life/HEALTH";
        workItemEntity.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        when(em.find(WorkItemEntity.class, workItemId)).thenReturn(workItemEntity);
        var created = new CalendarEvent("gcal-1", "primary", "[HEALTH] GP check-up",
                "casehub:domain:HEALTH", null,
                new EventTiming.AllDay(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)),
                List.of(), null);
        when(calendarPlatform.createEvent(eq("primary"), any())).thenReturn(created);

        var openItem = WorkItem.builder().id(workItemId).status(WorkItemStatus.PENDING).build();
        observer.onWorkItemEvent(WorkItemLifecycleEvent.of("CREATED", openItem, "life-system", null));

        var completedItem = WorkItem.builder().id(workItemId).status(WorkItemStatus.COMPLETED).build();
        observer.onWorkItemEvent(WorkItemLifecycleEvent.of("COMPLETED", completedItem, "life-system", null));

        verify(calendarPlatform).deleteEvent("primary", "gcal-1");
    }
}
