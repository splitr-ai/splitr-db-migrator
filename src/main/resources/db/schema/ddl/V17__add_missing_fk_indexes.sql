-- Phase 1: Missing FK indexes (HIGH-1)
DROP INDEX IF EXISTS idx_groups_created_by;
CREATE INDEX IF NOT EXISTS idx_groups_created_by ON groups (created_by);
CREATE INDEX IF NOT EXISTS idx_users_referred_by ON users (referred_by);
CREATE INDEX IF NOT EXISTS idx_categories_created_by ON categories (created_by);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_settlements_payer_guest_id ON settlements (payer_guest_id);
CREATE INDEX IF NOT EXISTS idx_settlements_payee_guest_id ON settlements (payee_guest_id);
CREATE INDEX IF NOT EXISTS idx_settlements_created_by ON settlements (created_by);
CREATE INDEX IF NOT EXISTS idx_activity_log_expense_id ON activity_log (expense_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_settlement_id ON activity_log (settlement_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_actor_guest_id ON activity_log (actor_guest_id);
CREATE INDEX IF NOT EXISTS idx_expenses_created_by_guest ON expenses (created_by_guest);
CREATE INDEX IF NOT EXISTS idx_notification_log_group_id ON notification_log (group_id);
