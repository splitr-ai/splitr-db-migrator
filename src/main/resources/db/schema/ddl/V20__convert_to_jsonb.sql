-- Phase 4: JSONB data type conversion
ALTER TABLE expenses ALTER COLUMN input_metadata TYPE JSONB USING input_metadata::jsonb;
ALTER TABLE activity_log ALTER COLUMN details TYPE JSONB USING details::jsonb;
ALTER TABLE notification_log ALTER COLUMN data_payload TYPE JSONB USING data_payload::jsonb;
ALTER TABLE users ALTER COLUMN preferences TYPE JSONB USING preferences::jsonb;
