-- DB-22 (BE-32): account deletion state on users.
-- deleted_at: set with is_deleted = true when the user deletes the account. NULL for an active user.
-- clerk_deleted_at: set when the Clerk user is confirmed deleted. NULL while the retry job must still call Clerk.
-- The CHECKs are added NOT VALID so no table scan runs under this ALTER's lock; V59 validates them.

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMPTZ NULL,
    ADD COLUMN clerk_deleted_at TIMESTAMPTZ NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_deleted_state
    CHECK (is_deleted = (deleted_at IS NOT NULL)) NOT VALID;

ALTER TABLE users
    ADD CONSTRAINT chk_users_clerk_deleted
    CHECK (clerk_deleted_at IS NULL OR deleted_at IS NOT NULL) NOT VALID;

COMMENT ON COLUMN users.deleted_at IS 'BE-32: time of account deletion. Set with is_deleted = true. The row is kept and anonymised.';
COMMENT ON COLUMN users.clerk_deleted_at IS 'BE-32: time the Clerk user was confirmed deleted. NULL after deletion means the retry job must call Clerk.';
