-- BE-10: Fix exchange_rates.effective_date type from TIMESTAMPTZ to DATE
-- ECB/Frankfurter rates are daily — DATE is the correct type.
-- V19 mistakenly converted this column to TIMESTAMPTZ along with other timestamp columns.

ALTER TABLE exchange_rates ALTER COLUMN effective_date TYPE DATE USING effective_date::date;
