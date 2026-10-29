CREATE TABLE coordination_template (
    id            UUID         NOT NULL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    domain        VARCHAR(255) NOT NULL,
    template_yaml TEXT         NOT NULL,
    description   TEXT,
    curated       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL,
    use_count     INT          NOT NULL DEFAULT 0,
    tenancy_id    VARCHAR(255) NOT NULL
);

CREATE INDEX idx_coordination_template_domain ON coordination_template(domain);
