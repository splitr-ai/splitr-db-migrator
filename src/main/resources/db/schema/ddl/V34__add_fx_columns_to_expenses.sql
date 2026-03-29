-- BE-10: Add FX conversion columns to expenses table
-- These are nullable — only populated when expense currency differs from group default.

ALTER TABLE expenses ADD COLUMN converted_amount BIGINT;
ALTER TABLE expenses ADD COLUMN converted_currency VARCHAR(3);
ALTER TABLE expenses ADD COLUMN fx_snapshot_id VARCHAR(60);

ALTER TABLE expenses ADD CONSTRAINT chk_expenses_converted_currency
    CHECK (converted_currency IS NULL OR converted_currency ~ '^[A-Z]{3}$');

ALTER TABLE expenses ADD CONSTRAINT fk_expenses_fx_snapshot
    FOREIGN KEY (fx_snapshot_id) REFERENCES fx_snapshots(id);
