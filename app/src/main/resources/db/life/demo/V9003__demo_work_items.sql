-- Demo WorkItems for Household Hub dashboard.
-- Mix of overdue, due-today, due-soon, and future items across domains.
-- Tenancy: 278776f9-e1b0-46fb-9032-8bddebdcf9ce (canonical demo tenant)

-- 1. Overdue: Approve boiler invoice (contractor, 2 days overdue)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000001', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Approve £450 boiler service invoice', 'Bob''s Plumbing completed the annual boiler service. Invoice attached for approval.',
        'PENDING', 'HIGH', 'household-admin', 'casehubio/life/CONTRACTOR_COORDINATION',
        NOW() - INTERVAL '5' DAY, NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000001', 'CONTRACTOR_COORDINATION', 'a0000000-0000-0000-0000-000000000001');

-- 2. Overdue: GP follow-up for Jean (elder care, 3 days overdue)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000002', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'GP follow-up for Jean', 'Post-appointment follow-up: check blood pressure results and book next review.',
        'PENDING', 'HIGH', 'household-admin,household-member', 'casehubio/life/ELDER_CARE',
        NOW() - INTERVAL '10' DAY, NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000002', 'ELDER_CARE', 'a0000000-0000-0000-0000-000000000005');

-- 3. Due today: Respond to school trip consent
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000003', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Respond to school trip consent — Ella', 'Oakwood Primary requires signed consent by end of day for Year 10 geography trip.',
        'PENDING', 'MEDIUM', 'household-admin,household-member', 'casehubio/life/FAMILY_SCHEDULING',
        NOW() - INTERVAL '3' DAY, NOW(), NOW() + INTERVAL '6' HOUR, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000003', 'FAMILY_SCHEDULING', 'a0000000-0000-0000-0000-000000000004');

-- 4. Due today: Monthly direct debit review
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000004', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Monthly direct debit review', 'Review active direct debits. Ella''s swimming cancelled — check if DD is still active.',
        'PENDING', 'MEDIUM', 'household-admin', 'casehubio/life/FINANCE',
        NOW() - INTERVAL '1' DAY, NOW(), NOW() + INTERVAL '8' HOUR, 0, 0);

INSERT INTO life_task_context (work_item_id, domain)
VALUES ('b0000000-0000-0000-0000-000000000004', 'FINANCE');

-- 5. Due soon: Confirm plumber appointment
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000005', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Confirm plumber appointment — Thursday 2pm', 'Bob''s Plumbing confirmed Thursday 2pm for boiler check. Acknowledge or reschedule.',
        'PENDING', 'MEDIUM', 'household-admin,household-member', 'casehubio/life/CONTRACTOR_COORDINATION',
        NOW() - INTERVAL '2' DAY, NOW(), NOW() + INTERVAL '2' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000005', 'CONTRACTOR_COORDINATION', 'a0000000-0000-0000-0000-000000000001');

-- 6. Due soon: Legal deadline — contract renewal response
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000006', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Respond to lease renewal — Harris & Co', 'Solicitor requires response by end of month. Review terms and confirm or negotiate.',
        'PENDING', 'HIGH', 'household-admin', 'casehubio/life/LEGAL',
        NOW() - INTERVAL '7' DAY, NOW(), NOW() + INTERVAL '5' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000006', 'LEGAL', 'a0000000-0000-0000-0000-000000000003');

-- 7. Normal: Book Tom's football kit replacement
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000007', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Order replacement football kit — Tom', 'Tom''s boots are too small. Order new pair before Saturday match.',
        'PENDING', 'LOW', 'household-admin,household-member', 'casehubio/life/HOUSEHOLD',
        NOW() - INTERVAL '1' DAY, NOW(), NOW() + INTERVAL '10' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain)
VALUES ('b0000000-0000-0000-0000-000000000007', 'HOUSEHOLD');

-- 8. Normal: Book dentist for Ella (health)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000008', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Book dentist appointment — Ella', 'Ella''s 6-month dental check-up is overdue. Book with usual practice.',
        'PENDING', 'MEDIUM', 'household-admin,household-member', 'casehubio/life/HEALTH',
        NOW() - INTERVAL '4' DAY, NOW(), NOW() + INTERVAL '14' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000008', 'HEALTH', 'a0000000-0000-0000-0000-000000000002');

-- 9. In progress: Jean's carer schedule review (elder care)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000009', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Review Jean''s carer schedule — August', 'Monthly carer schedule review. Maria has requested a Thursday shift swap.',
        'IN_PROGRESS', 'MEDIUM', 'household-admin', 'casehubio/life/ELDER_CARE',
        NOW() - INTERVAL '6' DAY, NOW(), NOW() + INTERVAL '7' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain, external_actor_id)
VALUES ('b0000000-0000-0000-0000-000000000009', 'ELDER_CARE', 'a0000000-0000-0000-0000-000000000005');

-- 10. Completed: Summer holiday travel booking (closed 2 days ago)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, completed_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000010', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Book summer holiday flights', 'Family flights to Malaga booked and confirmed.',
        'COMPLETED', 'HIGH', 'household-admin', 'casehubio/life/TRAVEL',
        NOW() - INTERVAL '14' DAY, NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain)
VALUES ('b0000000-0000-0000-0000-000000000010', 'TRAVEL');

-- 11. Ella-only task (junior visibility)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000011', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Return library books', 'Three library books due back this week.',
        'PENDING', 'LOW', 'household-junior', 'casehubio/life/HOUSEHOLD',
        NOW() - INTERVAL '2' DAY, NOW(), NOW() + INTERVAL '5' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain)
VALUES ('b0000000-0000-0000-0000-000000000011', 'HOUSEHOLD');

-- 12. Tom-only task (junior visibility)
INSERT INTO work_item (id, tenancy_id, title, description, status, priority, candidate_groups, scope,
                       created_at, updated_at, expires_at, version, accumulated_unclaimed_seconds)
VALUES ('b0000000-0000-0000-0000-000000000012', '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
        'Pack football bag for Saturday', 'Match kit, shin pads, water bottle, snack.',
        'PENDING', 'LOW', 'household-junior', 'casehubio/life/HOUSEHOLD',
        NOW(), NOW(), NOW() + INTERVAL '4' DAY, 0, 0);

INSERT INTO life_task_context (work_item_id, domain)
VALUES ('b0000000-0000-0000-0000-000000000012', 'HOUSEHOLD');
