package io.casehub.life.app.service;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.response.BriefingItem;
import io.casehub.life.api.response.BriefingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LifeDomainContextProviderTest {

    private DashboardService dashboardService;
    private LifeDomainContextProvider provider;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        provider = new LifeDomainContextProvider(dashboardService);
    }

    @Test
    void buildContextSummaryIncludesBriefingItems() {
        when(dashboardService.buildBriefing()).thenReturn(
            new BriefingResponse("Good morning", 2, List.of(
                new BriefingItem("GP appointment overdue", LifeDomain.HEALTH, "sla-breach"),
                new BriefingItem("Grocery delivery due today", LifeDomain.HOUSEHOLD, "action")
            ))
        );

        String summary = provider.buildContextSummary();
        assertTrue(summary.contains("GP appointment overdue"));
        assertTrue(summary.contains("Grocery delivery due today"));
        assertTrue(summary.contains("2"));
    }

    @Test
    void buildContextSummaryIncludesDomainLabels() {
        when(dashboardService.buildBriefing()).thenReturn(
            new BriefingResponse("Hello", 1, List.of(
                new BriefingItem("Dentist appointment", LifeDomain.HEALTH, "action")
            ))
        );

        String summary = provider.buildContextSummary();
        assertTrue(summary.contains("HEALTH"));
    }

    @Test
    void invalidateSetsFlag() {
        assertFalse(provider.isDirty());
        provider.invalidate();
        assertTrue(provider.isDirty());
    }

    @Test
    void buildContextSummaryClearsDirtyFlag() {
        when(dashboardService.buildBriefing()).thenReturn(
            new BriefingResponse("Hello", 0, List.of()));

        provider.invalidate();
        assertTrue(provider.isDirty());
        provider.buildContextSummary();
        assertFalse(provider.isDirty());
    }

    @Test
    void emptyBriefingProducesMinimalSummary() {
        when(dashboardService.buildBriefing()).thenReturn(
            new BriefingResponse("Hello", 0, List.of()));

        String summary = provider.buildContextSummary();
        assertTrue(summary.contains("Active items: 0"));
        assertFalse(summary.contains("Attention Items"));
    }

    @Test
    void largeItemListIsCapped() {
        var items = new java.util.ArrayList<BriefingItem>();
        for (int i = 0; i < 20; i++) {
            items.add(new BriefingItem("Task " + i, LifeDomain.HOUSEHOLD, "action"));
        }
        when(dashboardService.buildBriefing()).thenReturn(
            new BriefingResponse("Hello", 20, items));

        String summary = provider.buildContextSummary();
        assertTrue(summary.contains("more (use tools to query)"));
    }
}
