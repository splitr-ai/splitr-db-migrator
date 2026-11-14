-- DB-18 (BE-30): partial FK index (most rows stay NULL).

-- A failed CONCURRENTLY build leaves an INVALID index with this name; drop it so a retry works.
DROP INDEX CONCURRENTLY IF EXISTS idx_settlements_updated_by;

CREATE INDEX CONCURRENTLY idx_settlements_updated_by
    ON settlements (updated_by)
    WHERE updated_by IS NOT NULL;
