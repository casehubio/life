CREATE TABLE life_calendar_conflict (
    id                UUID        PRIMARY KEY,
    mapping_id        UUID        NOT NULL REFERENCES life_calendar_mapping(id),
    conflict_type     VARCHAR(20) NOT NULL,
    detected_at       TIMESTAMP   NOT NULL,
    external_summary  VARCHAR(500),
    external_timing   TEXT,
    status            VARCHAR(20) NOT NULL,
    resolved_at       TIMESTAMP,
    resolved_by       VARCHAR(255),
    domain            VARCHAR(50) NOT NULL
);

CREATE INDEX idx_calendar_conflict_status ON life_calendar_conflict(status);
CREATE INDEX idx_calendar_conflict_mapping ON life_calendar_conflict(mapping_id);
