CREATE TABLE household (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    timezone      VARCHAR(64) NOT NULL DEFAULT 'UTC',
    jurisdiction  VARCHAR(10),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE household_member (
    id                   UUID PRIMARY KEY,
    household_id         UUID NOT NULL REFERENCES household(id),
    keycloak_user_id     VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    email                VARCHAR(255),
    role                 VARCHAR(32) NOT NULL,
    relationship         VARCHAR(32),
    related_to           UUID REFERENCES household_member(id),
    notification_channel VARCHAR(32),
    notification_value   VARCHAR(255),
    joined_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at       TIMESTAMP
);

CREATE INDEX idx_household_member_household ON household_member(household_id);
