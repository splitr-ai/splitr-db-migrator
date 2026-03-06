-- GroupMembers
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_guest_user_id ON group_members(guest_user_id);

-- Expenses
CREATE INDEX IF NOT EXISTS idx_expenses_group_id ON expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_expenses_created_by ON expenses(created_by);
CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON expenses(category_id);

-- ExpensePayers
CREATE INDEX IF NOT EXISTS idx_expense_payers_expense_id ON expense_payers(expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_payers_user_id ON expense_payers(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_payers_guest_user_id ON expense_payers(guest_user_id);

-- ExpenseSplits
CREATE INDEX IF NOT EXISTS idx_expense_splits_expense_id ON expense_splits(expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_user_id ON expense_splits(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_guest_user_id ON expense_splits(guest_user_id);

-- Settlements
CREATE INDEX IF NOT EXISTS idx_settlements_group_id ON settlements(group_id);
CREATE INDEX IF NOT EXISTS idx_settlements_payer_user_id ON settlements(payer_user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_payee_user_id ON settlements(payee_user_id);

-- ActivityLog
CREATE INDEX IF NOT EXISTS idx_activity_log_group_id ON activity_log(group_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_actor_user_id ON activity_log(actor_user_id);
