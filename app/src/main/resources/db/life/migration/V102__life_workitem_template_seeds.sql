-- Seed life-domain WorkItemTemplates. These give foundation WorkItems their life-domain identity.
-- Runs after casehub-work V1-V31 (work_item_template table created at V5).
-- Flyway sorts by version number: work V1-V31 run before life V100-V102.
-- gen_random_uuid() available in H2 MODE=PostgreSQL and PostgreSQL.

INSERT INTO work_item_template
    (id, tenancy_id, name, description, priority, candidate_groups,
     default_expiry_hours, version, created_by, created_at)
VALUES
    (gen_random_uuid(),
     '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
     'household-task',
     'Routine household coordination task',
     'MEDIUM', 'household-member',
     24, 1, 'life-system', now()),
    (gen_random_uuid(),
     '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
     'health-appointment',
     'Health appointment or follow-up',
     'MEDIUM', 'household-member',
     48, 1, 'life-system', now()),
    (gen_random_uuid(),
     '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
     'contractor-coordination',
     'Contractor task with commitment tracking',
     'MEDIUM', 'household-member',
     72, 1, 'life-system', now());
