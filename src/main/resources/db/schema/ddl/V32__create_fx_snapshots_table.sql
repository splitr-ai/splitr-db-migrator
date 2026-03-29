-- BE-10: FX Snapshots table — immutable per-transaction conversion records
-- Each row captures the exact rate, source, and converted amount at the time of a cross-currency write.
-- Never updated after creation. Linked to expense/settlement via correlation_id.

CREATE TABLE fx_snapshots (
    id                VARCHAR(60)    PRIMARY KEY,
    base_currency     VARCHAR(3)     NOT NULL,
    quote_currency    VARCHAR(3)     NOT NULL,
    rate              NUMERIC(18, 8) NOT NULL,
    rate_source       VARCHAR(50)    NOT NULL,
    quoted_at         TIMESTAMPTZ    NOT NULL,
    rounding_mode     VARCHAR(20)    NOT NULL DEFAULT 'HALF_EVEN',
    scale             INTEGER        NOT NULL DEFAULT 0,
    original_amount   BIGINT         NOT NULL,
    converted_amount  BIGINT         NOT NULL,
    correlation_id    VARCHAR(60)    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_fx_snapshots_base_currency CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_fx_snapshots_quote_currency CHECK (quote_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_fx_snapshots_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_fx_snapshots_original_amount_positive CHECK (original_amount > 0),
    CONSTRAINT chk_fx_snapshots_converted_amount_positive CHECK (converted_amount > 0),
    CONSTRAINT chk_fx_snapshots_rounding_mode CHECK (rounding_mode IN ('HALF_EVEN', 'HALF_UP', 'HALF_DOWN', 'CEILING', 'FLOOR'))
);

CREATE INDEX idx_fx_snapshots_correlation ON fx_snapshots (correlation_id);
