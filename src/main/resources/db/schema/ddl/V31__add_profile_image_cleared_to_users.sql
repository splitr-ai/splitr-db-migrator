-- DB-3 / BE-3: Prevents Clerk avatar restore after user explicitly deletes their profile image.
-- When true, Clerk avatar sync is suppressed.

ALTER TABLE users ADD COLUMN profile_image_cleared BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.profile_image_cleared IS 'When true, Clerk avatar sync is suppressed because user explicitly deleted their profile image';
