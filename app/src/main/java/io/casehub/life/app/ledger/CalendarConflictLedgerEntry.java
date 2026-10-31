package io.casehub.life.app.ledger;

import io.casehub.ledger.jpa.JpaLedgerEntry;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Entity
@Table(name = "calendar_conflict_ledger_entry")
@DiscriminatorValue("CALENDAR_CONFLICT")
public class CalendarConflictLedgerEntry extends JpaLedgerEntry {

    @Column(name = "conflict_id", nullable = false)
    public UUID conflictId;

    @Column(name = "conflict_type", nullable = false, length = 20)
    public String conflictType;

    @Column(nullable = false, length = 50)
    public String domain;

    @Column(nullable = false, length = 20)
    public String resolution;

    @Column(name = "entity_type", nullable = false, length = 30)
    public String entityType;

    @Column(name = "entity_id", nullable = false)
    public String entityId;

    @Override
    protected byte[] domainContentBytes() {
        return String.join("|",
                conflictId != null ? conflictId.toString() : "",
                conflictType != null ? conflictType : "",
                domain != null ? domain : "",
                resolution != null ? resolution : "",
                entityType != null ? entityType : "",
                entityId != null ? entityId : ""
        ).getBytes(StandardCharsets.UTF_8);
    }
}
