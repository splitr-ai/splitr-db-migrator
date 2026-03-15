-- Phase 4: CHECK constraints (MED-4)
-- Drop existing constraints that are being replaced/supplemented with new naming/logic
ALTER TABLE group_members DROP CONSTRAINT IF EXISTS chk_group_members_user_or_guest;
ALTER TABLE expense_payers DROP CONSTRAINT IF EXISTS chk_expense_payers_user_or_guest;
ALTER TABLE expense_splits DROP CONSTRAINT IF EXISTS chk_expense_splits_user_or_guest;

ALTER TABLE group_members ADD CONSTRAINT chk_member_identity CHECK (user_id IS NOT NULL OR guest_user_id IS NOT NULL);
ALTER TABLE expense_payers ADD CONSTRAINT chk_payer_identity CHECK (user_id IS NOT NULL OR guest_user_id IS NOT NULL);
ALTER TABLE expense_splits ADD CONSTRAINT chk_split_identity CHECK (user_id IS NOT NULL OR guest_user_id IS NOT NULL);
