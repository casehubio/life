-- Consolidated initial schema for casehub-life ledger join tables.
-- Replaces V2100–V2106 incremental migrations (no production database exists).

CREATE TABLE health_decision_ledger_entry (
    id            UUID         NOT NULL,
    work_item_id  UUID         NOT NULL,
    provider_id   UUID,
    task_category VARCHAR(100) NOT NULL,
    sla_deadline  TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type    VARCHAR(30)  NOT NULL,
    outcome       VARCHAR(255),
    CONSTRAINT pk_health_decision_ledger_entry PRIMARY KEY (id),
    CONSTRAINT fk_health_decision_base FOREIGN KEY (id) REFERENCES ledger_entry (id)
);

CREATE TABLE financial_decision_ledger_entry (
    id                UUID          NOT NULL,
    work_item_id      UUID,
    oversight_ref     UUID          NOT NULL,
    amount_threshold  NUMERIC(15,2) NOT NULL,
    purchase_category VARCHAR(100)  NOT NULL,
    approved_by       VARCHAR(255),
    event_type        VARCHAR(30)   NOT NULL,
    CONSTRAINT pk_financial_decision_ledger_entry PRIMARY KEY (id),
    CONSTRAINT fk_financial_decision_base FOREIGN KEY (id) REFERENCES ledger_entry (id)
);

CREATE TABLE legal_action_ledger_entry (
    id               UUID         NOT NULL,
    work_item_id     UUID         NOT NULL,
    legal_obligation VARCHAR(255) NOT NULL,
    filing_deadline  TIMESTAMP WITH TIME ZONE NOT NULL,
    jurisdiction     VARCHAR(10),
    event_type       VARCHAR(30)  NOT NULL,
    action_taken     VARCHAR(255),
    CONSTRAINT pk_legal_action_ledger_entry PRIMARY KEY (id),
    CONSTRAINT fk_legal_action_base FOREIGN KEY (id) REFERENCES ledger_entry (id)
);

CREATE TABLE external_actor_erasure_ledger_entry (
    id                      UUID         NOT NULL,
    erased_actor_id         UUID         NOT NULL,
    contact_method          VARCHAR(50)  NOT NULL,
    erased_by               VARCHAR(255) NOT NULL,
    memory_records_erased   INTEGER      NOT NULL DEFAULT 0,
    ledger_entries_affected BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_external_actor_erasure_ledger_entry PRIMARY KEY (id),
    CONSTRAINT fk_external_actor_erasure_base FOREIGN KEY (id) REFERENCES ledger_entry (id)
);
