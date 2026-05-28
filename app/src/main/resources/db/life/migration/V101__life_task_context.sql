-- life_task_context: domain supplement for WorkItems created in the life domain.
-- work_item_id is a raw UUID cross-reference (no FK to casehub-work — cross-schema).
-- external_actor_id FK is within the casehub-life schema.
CREATE TABLE life_task_context (
    work_item_id      UUID         NOT NULL,
    domain            VARCHAR(50)  NOT NULL,
    external_actor_id UUID,
    recurrence        VARCHAR(100),
    CONSTRAINT pk_life_task_context PRIMARY KEY (work_item_id),
    CONSTRAINT fk_ltc_external_actor
        FOREIGN KEY (external_actor_id) REFERENCES external_actor(id)
);

CREATE INDEX idx_ltc_external_actor ON life_task_context (external_actor_id);
