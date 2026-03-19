-- Idempotency keys table for deduplicating POST requests
-- Keys expire after 24 hours and are pruned by a scheduled reaper job
-- See ADR-001: docs/adr/ADR-001-idempotency-layer.md

CREATE TABLE idempotency_keys (
    id                  VARCHAR(60)     PRIMARY KEY,
    user_id             VARCHAR(60)     NOT NULL,
    idempotency_key     VARCHAR(255)    NOT NULL,
    request_fingerprint VARCHAR(64),
    request_path        VARCHAR(512),
    response_status     INTEGER,
    response_body       TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING',
    locked_at           TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_idem_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT chk_idem_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idem_expires_at ON idempotency_keys (expires_at);
CREATE INDEX idx_idem_status_lock ON idempotency_keys (status, locked_at);

COMMENT ON TABLE idempotency_keys IS 'Stores idempotency keys for deduplicating POST requests. Keys expire after 24 hours.';
