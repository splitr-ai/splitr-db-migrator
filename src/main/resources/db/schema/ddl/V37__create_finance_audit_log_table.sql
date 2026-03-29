-- BE-9: Finance audit log — append-only audit trail for money-mutating operations
-- Tracks expense/settlement create, update, delete with correlation IDs.
-- No soft-delete, no version — this table is immutable by design.

CREATE TABLE finance_audit_log (
    id                VARCHAR(60)    PRIMARY KEY,
    correlation_id    VARCHAR(60)    NOT NULL,
    operation_type    VARCHAR(30)    NOT NULL,
    status            VARCHAR(10)    NOT NULL,
    entity_id         VARCHAR(60),
    entity_type       VARCHAR(20)    NOT NULL,
    group_id          VARCHAR(60)    NOT NULL,
    actor_user_id     VARCHAR(60)    NOT NULL,
    amount_cents      BIGINT,
    currency          VARCHAR(3),
    metadata          JSONB,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_fal_status CHECK (status IN ('STARTED', 'SUCCESS', 'FAILURE')),
    CONSTRAINT chk_fal_entity_type CHECK (entity_type IN ('expense', 'settlement')),
    CONSTRAINT chk_fal_operation_type CHECK (operation_type IN (
        'expense_create', 'expense_update', 'expense_delete',
        'settlement_create', 'settlement_update', 'settlement_delete'
    )),
    CONSTRAINT chk_fal_currency CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_fal_correlation_id ON finance_audit_log (correlation_id);
CREATE INDEX idx_fal_entity ON finance_audit_log (entity_id, entity_type);
CREATE INDEX idx_fal_group_time ON finance_audit_log (group_id, created_at DESC);
CREATE INDEX idx_fal_actor_time ON finance_audit_log (actor_user_id, created_at DESC);
