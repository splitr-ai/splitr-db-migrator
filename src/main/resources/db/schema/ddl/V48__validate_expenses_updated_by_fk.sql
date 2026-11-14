-- DB-17 (BE-30): validate the FK from V47. Takes SHARE UPDATE EXCLUSIVE, so reads and writes continue.

ALTER TABLE expenses
    VALIDATE CONSTRAINT fk_expenses_updated_by;
