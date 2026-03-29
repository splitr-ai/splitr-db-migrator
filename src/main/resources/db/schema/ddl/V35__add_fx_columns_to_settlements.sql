-- BE-10: Add FX conversion columns to settlements table
-- These are nullable — only populated when settlement currency differs from group default.

ALTER TABLE settlements ADD COLUMN converted_amount BIGINT;
ALTER TABLE settlements ADD COLUMN converted_currency VARCHAR(3);
ALTER TABLE settlements ADD COLUMN fx_snapshot_id VARCHAR(60);

ALTER TABLE settlements ADD CONSTRAINT chk_settlements_converted_currency
    CHECK (converted_currency IS NULL OR converted_currency ~ '^[A-Z]{3}$');

ALTER TABLE settlements ADD CONSTRAINT fk_settlements_fx_snapshot
    FOREIGN KEY (fx_snapshot_id) REFERENCES fx_snapshots(id);
