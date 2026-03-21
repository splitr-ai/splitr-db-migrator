-- DB-2 / GAP-7: Email digest — track when last digest was sent per user
-- Single nullable TIMESTAMPTZ column. No index needed — we page through users, not query by this column.

ALTER TABLE users ADD COLUMN last_digest_sent_at TIMESTAMPTZ;

COMMENT ON COLUMN users.last_digest_sent_at IS 'Timestamp of the last email digest sent to this user';
