-- DB-22 (BE-32): partial index for AccountDeletionCleanupJob.
-- The job query uses the same predicate:
--   WHERE deleted_at IS NOT NULL AND (clerk_deleted_at IS NULL OR profile_image_key IS NOT NULL)
-- The index holds only accounts with pending clean-up, so it stays near empty.

-- A failed CONCURRENTLY build leaves an INVALID index with this name; drop it so a retry works.
DROP INDEX CONCURRENTLY IF EXISTS idx_users_deletion_cleanup;

CREATE INDEX CONCURRENTLY idx_users_deletion_cleanup
    ON users (deleted_at)
    WHERE deleted_at IS NOT NULL
      AND (clerk_deleted_at IS NULL OR profile_image_key IS NOT NULL);
