-- Rename cognito_sub to clerk_id and make it NOT NULL
ALTER TABLE users RENAME COLUMN cognito_sub TO clerk_id;

-- For any existing rows (dev/test data), populate clerk_id from id before adding NOT NULL
UPDATE users SET clerk_id = id WHERE clerk_id IS NULL;

ALTER TABLE users ALTER COLUMN clerk_id SET NOT NULL;
