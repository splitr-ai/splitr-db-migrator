-- DB-22 (BE-32): validate the CHECKs from V58. Takes SHARE UPDATE EXCLUSIVE, so reads and writes continue.

ALTER TABLE users
    VALIDATE CONSTRAINT chk_users_deleted_state;

ALTER TABLE users
    VALIDATE CONSTRAINT chk_users_clerk_deleted;
