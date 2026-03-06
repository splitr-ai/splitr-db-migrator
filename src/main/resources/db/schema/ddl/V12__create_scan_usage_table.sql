CREATE TABLE scan_usage (
    id VARCHAR(60) PRIMARY KEY,
    user_id VARCHAR(60) NOT NULL REFERENCES users(id),
    scanned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_scan_usage_user_scanned ON scan_usage(user_id, scanned_at);
