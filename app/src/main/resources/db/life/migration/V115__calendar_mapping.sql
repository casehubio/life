CREATE TABLE life_calendar_mapping (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    calendar_platform_id VARCHAR(50) NOT NULL,
    calendar_id VARCHAR(255) NOT NULL,
    calendar_event_id VARCHAR(255) NOT NULL,
    domain VARCHAR(50) NOT NULL,
    summary VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(entity_type, entity_id)
);
