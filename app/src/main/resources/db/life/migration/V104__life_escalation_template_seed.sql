-- Layer 3: life-escalation WorkItemTemplate.
-- Used by LifeWatchdogAlertObserver when a commitment deadline passes without response.
-- Routes to household-admin for manual resolution.

INSERT INTO work_item_template
    (id, tenancy_id, name, description, priority, candidate_groups,
     default_expiry_hours, version, created_by, created_at)
VALUES
    (gen_random_uuid(),
     '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
     'life-escalation',
     'Commitment deadline passed — manual action required by household-admin',
     'HIGH', 'household-admin',
     24, 1, 'life-system', now());
