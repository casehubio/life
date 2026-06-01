CREATE TABLE life_case_tracker (
    id         UUID         NOT NULL,
    case_type  VARCHAR(64)  NOT NULL,
    engine_case_id UUID,
    status     VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT pk_life_case_tracker PRIMARY KEY (id)
);

CREATE INDEX idx_life_case_tracker_type_status ON life_case_tracker (case_type, status);
CREATE UNIQUE INDEX uidx_life_case_tracker_engine_case_id ON life_case_tracker (engine_case_id);
