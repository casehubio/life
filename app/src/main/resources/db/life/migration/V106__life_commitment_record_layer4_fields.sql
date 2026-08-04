ALTER TABLE life_commitment_record ADD COLUMN approved_by VARCHAR(255);
ALTER TABLE life_commitment_record ADD COLUMN amount_threshold NUMERIC(15, 2);
ALTER TABLE life_commitment_record ADD COLUMN purchase_category VARCHAR(100);
