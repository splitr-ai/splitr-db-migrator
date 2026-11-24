-- Legal consent records: one row each time a user accepts a version of the
-- Terms and Conditions or the Privacy Policy.
-- Append-only evidence of consent (who, which version and text hash, when,
-- from where). Written only by POST /v1/users/me/legal-acceptances
-- (LegalAcceptanceService). Rows are never updated or soft-deleted, so the
-- table has no is_deleted / version columns. Rows outlive account deletion
-- for the retention period in the Privacy Policy (section 7), so user_id has
-- no ON DELETE CASCADE.

CREATE TABLE legal_acceptances (
    id                VARCHAR(60)  PRIMARY KEY,
    user_id           VARCHAR(60)  NOT NULL REFERENCES users(id),
    document_type     VARCHAR(20)  NOT NULL,
    document_version  VARCHAR(20)  NOT NULL,
    document_sha256   VARCHAR(64)  NOT NULL,
    age_confirmed     BOOLEAN      NOT NULL,
    method            VARCHAR(40)  NOT NULL,
    ip_address        VARCHAR(45),
    user_agent        TEXT,
    platform          VARCHAR(20),
    app_version       VARCHAR(40),
    accepted_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_legal_acceptances_document_type CHECK (document_type IN ('TERMS', 'PRIVACY')),
    CONSTRAINT chk_legal_acceptances_sha256 CHECK (document_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_legal_acceptances_age CHECK (age_confirmed),
    CONSTRAINT chk_legal_acceptances_platform CHECK (platform IS NULL OR platform IN ('ios', 'android', 'web')),
    CONSTRAINT uq_legal_acceptances_user_doc_version UNIQUE (user_id, document_type, document_version)
);

-- Latest acceptance per user and document (GET /v1/users/me/legal).
CREATE INDEX idx_legal_acceptances_user_doc_time
    ON legal_acceptances (user_id, document_type, accepted_at DESC);

COMMENT ON TABLE legal_acceptances IS 'Append-only record of each Terms / Privacy Policy version a user accepted';
COMMENT ON COLUMN legal_acceptances.document_sha256 IS 'SHA-256 (hex) of the exact document text accepted';
COMMENT ON COLUMN legal_acceptances.method IS 'How consent was given, e.g. checkbox_v1 (two unticked checkboxes)';
COMMENT ON COLUMN legal_acceptances.ip_address IS 'Client IP seen by the server at acceptance (IPv4 or IPv6)';
