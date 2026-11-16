-- Favourite groups: a per-user star on a group, stored on that user's membership row.
-- Written only by PUT / DELETE /v1/groups/{groupId}/favorite (GroupMemberService.setFavorite).
-- favorited_at is set while starred and NULL otherwise, so the two columns always agree.

-- Fail fast instead of queueing every read behind a blocked ALTER.
SET LOCAL lock_timeout = '5s';

ALTER TABLE group_members
    ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN favorited_at TIMESTAMPTZ NULL;

ALTER TABLE group_members
    ADD CONSTRAINT chk_group_members_favorited_at
        CHECK (is_favorite = (favorited_at IS NOT NULL));

COMMENT ON COLUMN group_members.is_favorite IS 'True when this member starred the group as a favourite';
COMMENT ON COLUMN group_members.favorited_at IS 'When the member starred the group; NULL when not a favourite';
