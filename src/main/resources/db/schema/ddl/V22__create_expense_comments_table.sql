CREATE TABLE expense_comments (
    id VARCHAR(60) PRIMARY KEY,
    expense_id VARCHAR(60) NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    author_user_id VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,
    author_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_comment_author CHECK (
        (author_user_id IS NOT NULL AND author_guest_id IS NULL) OR
        (author_user_id IS NULL AND author_guest_id IS NOT NULL)
    )
);

CREATE INDEX idx_expense_comments_expense_id ON expense_comments(expense_id) WHERE NOT is_deleted;
CREATE INDEX idx_expense_comments_author_user_id ON expense_comments(author_user_id) WHERE author_user_id IS NOT NULL AND NOT is_deleted;
CREATE INDEX idx_expense_comments_author_guest_id ON expense_comments(author_guest_id) WHERE author_guest_id IS NOT NULL AND NOT is_deleted;
