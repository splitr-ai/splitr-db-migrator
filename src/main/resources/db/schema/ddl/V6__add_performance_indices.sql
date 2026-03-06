-- Creating additional performance indices on key foreign keys
CREATE INDEX idx_expenses_group_id ON expenses(group_id);
CREATE INDEX idx_group_members_user_id_plain ON group_members(user_id);
CREATE INDEX idx_expense_payers_expense_id ON expense_payers(expense_id);
CREATE INDEX idx_expense_splits_expense_id ON expense_splits(expense_id);
