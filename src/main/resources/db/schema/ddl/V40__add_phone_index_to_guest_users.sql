-- DB-12: Add index on guest_users.phone for phone-based contact matching
-- Partial index: only indexes non-null phones
-- CONCURRENTLY: avoids locking the table during creation

CREATE INDEX CONCURRENTLY idx_guest_users_phone ON guest_users(phone) WHERE phone IS NOT NULL;
