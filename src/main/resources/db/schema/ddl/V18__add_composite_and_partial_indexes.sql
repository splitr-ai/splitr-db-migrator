-- Phase 2: Composite/partial indexes (HIGH-2, HIGH-3, MED-1, MED-5, MED-6, LOW-2)
DROP INDEX IF EXISTS idx_exchange_rates_lookup;
CREATE UNIQUE INDEX IF NOT EXISTS idx_exchange_rates_lookup ON exchange_rates (base_currency, target_currency, effective_date);
CREATE INDEX IF NOT EXISTS idx_expenses_group_active ON expenses (group_id, expense_date DESC) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_settlements_group_active ON settlements (group_id, settlement_date DESC) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_group_members_group_active ON group_members (group_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_group_members_user_active ON group_members (user_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_push_tokens_user_active ON push_tokens (user_id) WHERE is_active = true AND is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uq_group_members_group_user ON group_members (group_id, user_id) WHERE user_id IS NOT NULL AND is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uq_group_members_group_guest ON group_members (group_id, guest_user_id) WHERE guest_user_id IS NOT NULL AND is_deleted = false;
DROP INDEX IF EXISTS idx_guest_users_email;
CREATE INDEX IF NOT EXISTS idx_guest_users_email ON guest_users (email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notification_log_pending ON notification_log (created_at) WHERE delivery_status = 'pending';
CREATE INDEX IF NOT EXISTS idx_notification_log_sent_tickets ON notification_log (delivery_status, created_at) WHERE delivery_status = 'sent' AND expo_ticket_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_activity_log_group_created ON activity_log (group_id, created_at DESC);
