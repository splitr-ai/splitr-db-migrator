-- DB-19: one-time data fix. Before the BE-24..31 release, joining by invite could bind a user to a
-- guest membership that promotion had soft-deleted (is_deleted = true, user_id set). The old
-- membership check ignored is_deleted, so those users still had access. The new check requires an
-- active row, so reactivate those rows. uq_group_members_user (full unique on group_id, user_id)
-- allows one row per user per group, so no active duplicate can exist; the NOT EXISTS is defensive.

UPDATE group_members
SET is_deleted = false,
    version = version + 1,
    updated_at = NOW()
WHERE id IN (
    SELECT DISTINCT ON (d.group_id, d.user_id) d.id
    FROM group_members d
    WHERE d.is_deleted = true
      AND d.user_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM group_members a
          WHERE a.group_id = d.group_id
            AND a.user_id = d.user_id
            AND a.is_deleted = false
      )
    ORDER BY d.group_id, d.user_id, d.joined_at
);
