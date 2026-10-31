package io.casehub.life.app.observer;

import io.casehub.ledger.api.model.LedgerEntryType;
import io.casehub.ledger.api.spi.LedgerEntryRepository;
import io.casehub.life.app.event.CalendarConflictDetected;
import io.casehub.life.app.event.CalendarConflictResolved;
import io.casehub.life.app.ledger.CalendarConflictLedgerEntry;
import io.casehub.life.app.service.ledger.DomainLedgerHandler;
import io.casehub.platform.api.identity.ActorType;
import io.casehub.platform.api.identity.TenancyConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class LifeCalendarConflictLedgerObserver {

    private final LedgerEntryRepository ledgerRepository;

    @Inject
    public LifeCalendarConflictLedgerObserver(LedgerEntryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onConflictDetected(@Observes CalendarConflictDetected event) {
        var entry = new CalendarConflictLedgerEntry();
        entry.conflictId = event.conflictId();
        entry.conflictType = event.conflictType().name();
        entry.domain = event.domain().name();
        entry.resolution = "DETECTED";
        entry.entityType = "WORK_ITEM";
        entry.entityId = event.entityId();
        populateBase(entry, event.mappingId(), "life-system");
        ledgerRepository.save(entry, TenancyConstants.DEFAULT_TENANT_ID);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onConflictResolved(@Observes CalendarConflictResolved event) {
        var entry = new CalendarConflictLedgerEntry();
        entry.conflictId = event.conflictId();
        entry.conflictType = "RESOLVED";
        entry.domain = event.domain().name();
        entry.resolution = event.resolution().name();
        entry.entityType = "WORK_ITEM";
        entry.entityId = event.entityId();
        populateBase(entry, event.conflictId(), event.resolvedBy());
        ledgerRepository.save(entry, TenancyConstants.DEFAULT_TENANT_ID);
    }

    private void populateBase(CalendarConflictLedgerEntry entry, UUID subjectId, String actorId) {
        entry.subjectId = subjectId;
        entry.sequenceNumber = DomainLedgerHandler.nextSequenceNumber(ledgerRepository, subjectId);
        entry.entryType = LedgerEntryType.EVENT;
        entry.actorId = actorId;
        entry.actorType = ActorType.SYSTEM;
        entry.actorRole = "CalendarConflictObserver";
    }
}
