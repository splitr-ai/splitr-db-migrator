-- GAP-1: Recurring expense templates table
-- Stores recurring expense definitions with frequency, payer/split JSON, and scheduling metadata.
-- Extends BaseEntity (created_at, updated_at, is_deleted, version).

CREATE TABLE recurring_expense_templates (
    id                  VARCHAR(60)    PRIMARY KEY,
    group_id            VARCHAR(60)    NOT NULL REFERENCES groups(id),
    created_by          VARCHAR(60)    NOT NULL REFERENCES users(id),
    description         VARCHAR(255)   NOT NULL,
    total_amount        BIGINT         NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    category_id         VARCHAR(60)    REFERENCES categories(id),
    split_type          VARCHAR(20)    NOT NULL,
    notes               TEXT,
    frequency           VARCHAR(20)    NOT NULL,
    anchor_date         DATE           NOT NULL,
    next_run_date       DATE           NOT NULL,
    end_date            DATE,
    status              VARCHAR(20)    NOT NULL DEFAULT 'active',
    last_created_at     TIMESTAMPTZ,
    total_occurrences   INTEGER        NOT NULL DEFAULT 0,
    payers_json         JSONB          NOT NULL,
    splits_json         JSONB          NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    version             INTEGER        NOT NULL DEFAULT 0,

    CONSTRAINT chk_recur_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_recur_amount CHECK (total_amount > 0),
    CONSTRAINT chk_recur_split_type CHECK (split_type IN ('equal', 'exact', 'percentage', 'shares')),
    CONSTRAINT chk_recur_frequency CHECK (frequency IN ('daily', 'weekly', 'biweekly', 'monthly', 'yearly')),
    CONSTRAINT chk_recur_status CHECK (status IN ('active', 'paused', 'expired', 'cancelled')),
    CONSTRAINT chk_recur_occurrences CHECK (total_occurrences >= 0)
);

CREATE INDEX idx_recur_group_id ON recurring_expense_templates (group_id);
CREATE INDEX idx_recur_created_by ON recurring_expense_templates (created_by);
CREATE INDEX idx_recur_next_run_date ON recurring_expense_templates (next_run_date) WHERE status = 'active' AND is_deleted = FALSE;
