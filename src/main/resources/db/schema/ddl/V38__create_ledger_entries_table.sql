-- BE-14 / DB-10: Ledger-first finance architecture
-- Append-only ledger for deterministic net-balance computation and auditability.

CREATE TABLE ledger_entries (
    id              VARCHAR(60)  PRIMARY KEY,
    correlation_id  VARCHAR(60)  NOT NULL,
    group_id        VARCHAR(60)  NOT NULL,
    member_key      VARCHAR(80)  NOT NULL,
    amount          BIGINT       NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    operation_type  VARCHAR(10)  NOT NULL,
    source_type     VARCHAR(15)  NOT NULL,
    source_id       VARCHAR(60)  NOT NULL,
    source_version  INT          NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_ledger_operation CHECK (operation_type IN ('RECORD', 'REVERSAL')),
    CONSTRAINT chk_ledger_source CHECK (source_type IN ('EXPENSE', 'SETTLEMENT')),
    CONSTRAINT chk_ledger_nonzero CHECK (amount <> 0),
    CONSTRAINT chk_ledger_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_ledger_group_currency ON ledger_entries(group_id, currency);
CREATE INDEX idx_ledger_member_group ON ledger_entries(member_key, group_id, currency);
CREATE INDEX idx_ledger_source ON ledger_entries(source_id, source_version);
CREATE INDEX idx_ledger_correlation ON ledger_entries(correlation_id);
CREATE INDEX idx_ledger_group_time ON ledger_entries(group_id, created_at DESC);
