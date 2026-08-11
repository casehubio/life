-- Consolidated initial schema for casehub-life domain tables.
-- Replaces V100–V111 incremental migrations (no production database exists).

CREATE TABLE external_actor (
    id              UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    actor_type      VARCHAR(50)  NOT NULL,
    contact_method  VARCHAR(50)  NOT NULL,
    contact_value   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    gdpr_erased_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_external_actor PRIMARY KEY (id)
);

CREATE TABLE life_task_context (
    work_item_id      UUID         NOT NULL,
    domain            VARCHAR(50)  NOT NULL,
    external_actor_id UUID,
    recurrence        VARCHAR(100),
    jurisdiction      VARCHAR(10),
    CONSTRAINT pk_life_task_context PRIMARY KEY (work_item_id),
    CONSTRAINT fk_ltc_external_actor
        FOREIGN KEY (external_actor_id) REFERENCES external_actor(id)
);

CREATE INDEX idx_ltc_external_actor ON life_task_context (external_actor_id);

-- Seed life-domain WorkItemTemplates.
-- Runs after casehub-work V1–V31 (work_item_template table created at V5).
INSERT INTO work_item_template
    (id, name, description, category, priority, candidate_groups,
     default_expiry_hours, created_by, created_at)
VALUES
    (gen_random_uuid(),
     'household-task',
     'Routine household coordination task',
     'household', 'MEDIUM', 'household-member',
     24, 'life-system', now()),
    (gen_random_uuid(),
     'health-appointment',
     'Health appointment or follow-up',
     'health', 'MEDIUM', 'household-member',
     48, 'life-system', now()),
    (gen_random_uuid(),
     'contractor-coordination',
     'Contractor task with commitment tracking',
     'contractor', 'MEDIUM', 'household-member',
     72, 'life-system', now()),
    (gen_random_uuid(),
     'life-escalation',
     'Commitment deadline passed — manual action required by household-admin',
     'household', 'HIGH', 'household-admin',
     24, 'life-system', now());

CREATE TABLE life_commitment_record (
    id                UUID                     NOT NULL,
    correlation_id    VARCHAR(255)             NOT NULL,
    mode              VARCHAR(32)              NOT NULL,
    status            VARCHAR(32)              NOT NULL,
    work_item_id      UUID,
    external_actor_id UUID,
    delegate_to       VARCHAR(255),
    channel_id        VARCHAR(255)             NOT NULL,
    deadline          TIMESTAMP WITH TIME ZONE,
    pending_task_json TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by       VARCHAR(255),
    amount_threshold  NUMERIC(15, 2),
    purchase_category VARCHAR(100),
    domain            VARCHAR(50),
    oversight_key     VARCHAR(255),
    CONSTRAINT pk_life_commitment_record PRIMARY KEY (id),
    CONSTRAINT uq_life_commitment_correlation UNIQUE (correlation_id)
);

CREATE INDEX idx_life_commitment_work_item ON life_commitment_record (work_item_id);
CREATE INDEX idx_life_commitment_correlation ON life_commitment_record (correlation_id);
CREATE INDEX idx_life_commitment_channel_status
    ON life_commitment_record (channel_id, status);
CREATE UNIQUE INDEX uq_oversight_pending_key
    ON life_commitment_record (delegate_to)
    WHERE mode = 'OVERSIGHT' AND status = 'PENDING_RESPONSE';

CREATE TABLE life_case_tracker (
    id           UUID         NOT NULL,
    case_type    VARCHAR(64)  NOT NULL,
    engine_case_id UUID,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    domain       VARCHAR(32)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT pk_life_case_tracker PRIMARY KEY (id)
);

CREATE INDEX idx_life_case_tracker_type_status ON life_case_tracker (case_type, status);
CREATE UNIQUE INDEX uidx_life_case_tracker_engine_case_id ON life_case_tracker (engine_case_id);
