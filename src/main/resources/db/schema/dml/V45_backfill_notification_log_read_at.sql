-- DB-16 (BE-25): one-time backfill. Notifications sent before the inbox existed count as read,
-- so existing users do not open the app to a "99+" badge.
-- Runs before the partial unread index (V46) so the index is built once on the final data,
-- and these updates stay HOT (read_at is not yet indexed).

-- Fail fast if the delivery jobs hold row locks.
SET LOCAL lock_timeout = '5s';

UPDATE notification_log
SET read_at = created_at
WHERE read_at IS NULL;
