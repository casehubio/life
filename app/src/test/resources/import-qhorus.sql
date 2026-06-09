CREATE TABLE IF NOT EXISTS ledger_subject_sequence (
    subject_id UUID NOT NULL PRIMARY KEY,
    next_seq   INT  NOT NULL DEFAULT 1
);
