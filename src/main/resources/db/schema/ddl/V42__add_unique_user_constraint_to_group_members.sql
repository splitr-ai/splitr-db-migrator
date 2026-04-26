-- DB-14 (H2): Unique partial index to prevent duplicate user memberships per group.
-- Mirrors DB-13 but for real-user rows. Without this, two concurrent
-- invite-by-email / join-via-invite calls can create duplicate member rows
-- for the same user in the same group. The DataIntegrityViolationException
-- catch at GroupMemberService.java:407 + :497 depends on this constraint firing.
-- Partial: only applies to non-deleted memberships with a user_id.

CREATE UNIQUE INDEX CONCURRENTLY idx_group_members_unique_user
    ON group_members(group_id, user_id)
    WHERE user_id IS NOT NULL AND is_deleted = false;
