CREATE TABLE life_calendar_sync_state (
    id           UUID PRIMARY KEY,
    platform_id  VARCHAR(50)   NOT NULL,
    calendar_id  VARCHAR(255)  NOT NULL,
    sync_token   VARCHAR(1024),
    last_sync_at TIMESTAMP,
    UNIQUE (platform_id, calendar_id)
);

ALTER TABLE life_calendar_mapping
    ADD COLUMN pushed_timing TEXT;

ALTER TABLE life_calendar_mapping
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_calendar_mapping_event_id ON life_calendar_mapping(calendar_event_id);
