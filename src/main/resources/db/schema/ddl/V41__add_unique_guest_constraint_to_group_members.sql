-- DB-13: Unique partial index to prevent duplicate guest memberships per group
-- Prevents race condition where two concurrent invite requests create duplicate memberships
-- for the same guest in the same group.
-- Partial: only applies to non-deleted memberships with a guest_user_id.

CREATE UNIQUE INDEX CONCURRENTLY idx_group_members_unique_guest
    ON group_members(group_id, guest_user_id)
    WHERE guest_user_id IS NOT NULL AND is_deleted = false;
