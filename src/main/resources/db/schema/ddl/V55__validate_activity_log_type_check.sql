-- DB-20: validate the CHECK from V54. Takes SHARE UPDATE EXCLUSIVE, so reads and writes continue.

ALTER TABLE activity_log
    VALIDATE CONSTRAINT chk_activity_log_type;
