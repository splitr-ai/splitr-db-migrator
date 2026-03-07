-- Settlement nudge tracking table
 CREATE TABLE settlement_nudges (
     id VARCHAR(60) PRIMARY KEY,
     group_id VARCHAR(60) NOT NULL REFERENCES groups(id),
     debtor_user_id VARCHAR(60) REFERENCES users(id),
     debtor_guest_id VARCHAR(60) REFERENCES guest_users(id),
     creditor_user_id VARCHAR(60) REFERENCES users(id),
     creditor_guest_id VARCHAR(60) REFERENCES guest_users(id),
     currency VARCHAR(3) NOT NULL,
     nudge_type VARCHAR(20) NOT NULL,        -- 'automated' or 'manual'
     nudge_count INT NOT NULL DEFAULT 1,
     last_nudged_at TIMESTAMP NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
     CONSTRAINT chk_nudge_debtor CHECK (debtor_user_id IS NOT NULL OR debtor_guest_id IS NOT NULL),
     CONSTRAINT chk_nudge_creditor CHECK (creditor_user_id IS NOT NULL OR creditor_guest_id IS NOT NULL)
 );

 CREATE INDEX idx_nudge_group ON settlement_nudges(group_id);
 CREATE INDEX idx_nudge_debtor ON settlement_nudges(debtor_user_id, last_nudged_at);
 CREATE INDEX idx_nudge_creditor ON settlement_nudges(creditor_user_id);
 CREATE UNIQUE INDEX idx_nudge_unique_pair ON settlement_nudges(
     group_id,
     COALESCE(debtor_user_id, ''), COALESCE(debtor_guest_id, ''),
     COALESCE(creditor_user_id, ''), COALESCE(creditor_guest_id, ''),
     currency
 );
