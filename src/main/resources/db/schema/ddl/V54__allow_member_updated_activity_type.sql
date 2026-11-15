-- DB-20: allow 'member_updated' in activity_log.activity_type (PATCH /groups/{id}/members/{id}).
-- The new CHECK is added NOT VALID so no table scan runs under this ALTER's lock;
-- V55 validates it separately.

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE activity_log
    DROP CONSTRAINT chk_activity_log_type;

ALTER TABLE activity_log
    ADD CONSTRAINT chk_activity_log_type CHECK (
        activity_type IN (
            'expense_created',
            'expense_updated',
            'expense_deleted',
            'settlement_created',
            'settlement_updated',
            'settlement_deleted',
            'member_added',
            'member_joined',
            'member_left',
            'member_removed',
            'member_updated',
            'group_created',
            'group_updated',
            'group_archived',
            'group_unarchived',
            'group_deleted',
            'member_joined_via_invite',
            'invite_code_regenerated',
            'expense_comment_added'
        )
    ) NOT VALID;
