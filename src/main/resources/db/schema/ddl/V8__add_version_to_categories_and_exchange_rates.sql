-- categories and exchange_rates tables need a version column
ALTER TABLE categories ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
