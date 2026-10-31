CREATE TABLE calendar_conflict_ledger_entry (
    id             UUID         PRIMARY KEY REFERENCES ledger_entry(id),
    conflict_id    UUID         NOT NULL,
    conflict_type  VARCHAR(20)  NOT NULL,
    domain         VARCHAR(50)  NOT NULL,
    resolution     VARCHAR(20)  NOT NULL,
    entity_type    VARCHAR(30)  NOT NULL,
    entity_id      VARCHAR(255) NOT NULL
);
