-- DB-15 (H4): Add push_token_id to notification_log so pollExpoReceipts can
-- scope token deactivation to the exact device that failed. Previously, a
-- single DeviceNotRegistered receipt deactivated EVERY active token for the
-- recipient user (because the log didn't record which token was targeted),
-- logging them out across all devices.
-- Nullable: pre-H4 rows have no value and are handled by a fallback in code.
-- No index needed — field is looked up by notification_log row, not scanned.

ALTER TABLE notification_log
    ADD COLUMN push_token_id VARCHAR(60);
