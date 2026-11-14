-- DB-18 (BE-30): last user who edited the settlement; NULL until the first edit.
-- The FK is added NOT VALID so no table scan runs under this ALTER's lock;
-- V51 validates it separately.

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE settlements
    ADD COLUMN updated_by VARCHAR(60) NULL;

ALTER TABLE settlements
    ADD CONSTRAINT fk_settlements_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) NOT VALID;
