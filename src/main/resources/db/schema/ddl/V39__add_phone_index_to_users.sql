-- DB-11: Add index on users.phone for phone-based contact matching
-- Partial index: only indexes non-null phones (most rows may have null phone)
-- CONCURRENTLY: avoids locking the table during creation

CREATE INDEX CONCURRENTLY idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
