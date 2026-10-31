package io.casehub.life.app.observer;

import io.casehub.ledger.api.model.LedgerEntry;
import io.casehub.ledger.api.spi.LedgerEntryRepository;
import io.casehub.life.api.CalendarConflictStatus;
import io.casehub.life.api.CalendarConflictType;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.app.event.CalendarConflictDetected;
import io.casehub.life.app.event.CalendarConflictResolved;
import io.casehub.life.app.ledger.CalendarConflictLedgerEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifeCalendarConflictLedgerObserverTest {

    @Mock
    LedgerEntryRepository ledgerRepository;

    private LifeCalendarConflictLedgerObserver observer;

    @BeforeEach
    void setUp() {
        when(ledgerRepository.findLatestBySubjectId(any(UUID.class), anyString()))
                .thenReturn(java.util.Optional.empty());
        observer = new LifeCalendarConflictLedgerObserver(ledgerRepository);
    }

    @Test
    void onConflictDetected_writesLedgerEntry_withDetectedResolution() {
        var event = new CalendarConflictDetected(
                UUID.randomUUID(), CalendarConflictType.MODIFIED, LifeDomain.HEALTH,
                "wi-123", UUID.randomUUID());

        observer.onConflictDetected(event);

        var captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(captor.capture(), anyString());
        var entry = (CalendarConflictLedgerEntry) captor.getValue();
        assertThat(entry.conflictId).isEqualTo(event.conflictId());
        assertThat(entry.conflictType).isEqualTo("MODIFIED");
        assertThat(entry.domain).isEqualTo("HEALTH");
        assertThat(entry.resolution).isEqualTo("DETECTED");
        assertThat(entry.entityType).isEqualTo("WORK_ITEM");
        assertThat(entry.entityId).isEqualTo("wi-123");
        assertThat(entry.actorId).isEqualTo("life-system");
    }

    @Test
    void onConflictResolved_writesLedgerEntry_withResolutionStatus() {
        var event = new CalendarConflictResolved(
                UUID.randomUUID(), CalendarConflictStatus.ACCEPTED, "user-admin",
                LifeDomain.FINANCE, "wi-456");

        observer.onConflictResolved(event);

        var captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(captor.capture(), anyString());
        var entry = (CalendarConflictLedgerEntry) captor.getValue();
        assertThat(entry.conflictId).isEqualTo(event.conflictId());
        assertThat(entry.resolution).isEqualTo("ACCEPTED");
        assertThat(entry.actorId).isEqualTo("user-admin");
    }
}
