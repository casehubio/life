-- Precondition: no existing erasure entries (domainContentBytes() change is chain-breaking).
-- PL/pgSQL DO $$ block removed — H2 does not support it. Pre-release: no existing data.

-- Align jurisdiction column to ISO 3166-1/2 length (was 100, now 10)
ALTER TABLE legal_action_ledger_entry ALTER COLUMN jurisdiction VARCHAR(10);

-- Add ledger_entries_affected for self-contained erasure proof (#49)
ALTER TABLE external_actor_erasure_ledger_entry ADD COLUMN ledger_entries_affected BIGINT NOT NULL DEFAULT 0;
