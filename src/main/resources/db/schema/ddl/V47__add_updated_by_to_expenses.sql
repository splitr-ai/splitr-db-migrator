-- DB-17 (BE-30): last user who edited the expense; NULL until the first edit.
-- The FK is added NOT VALID so no table scan runs under this ALTER's lock;
-- V48 validates it separately.

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE expenses
    ADD COLUMN updated_by VARCHAR(60) NULL;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) NOT VALID;
