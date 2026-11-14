-- DB-16 (BE-25): partial index for the unread badge
-- (SELECT COUNT(*) FROM notification_log WHERE user_id = ? AND read_at IS NULL on every Home load).

-- A failed CONCURRENTLY build leaves an INVALID index with this name; drop it so a retry works.
DROP INDEX CONCURRENTLY IF EXISTS idx_notification_log_user_unread;

CREATE INDEX CONCURRENTLY idx_notification_log_user_unread
    ON notification_log (user_id)
    WHERE read_at IS NULL;
