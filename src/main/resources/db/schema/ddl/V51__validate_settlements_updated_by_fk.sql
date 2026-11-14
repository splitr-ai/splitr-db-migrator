-- DB-18 (BE-30): validate the FK from V50. Takes SHARE UPDATE EXCLUSIVE, so reads and writes continue.

ALTER TABLE settlements
    VALIDATE CONSTRAINT fk_settlements_updated_by;
