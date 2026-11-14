-- DB-16 (BE-25): in-app inbox read state. NULL while unread.
-- Written only by the BE mark-read bulk update (NotificationLogRepository.markReadByIds / markAllRead).

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE notification_log
    ADD COLUMN read_at TIMESTAMPTZ NULL;
