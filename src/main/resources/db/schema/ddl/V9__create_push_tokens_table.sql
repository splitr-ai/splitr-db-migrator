CREATE TABLE push_tokens (
    id              VARCHAR(60)     PRIMARY KEY,
    user_id         VARCHAR(60)     NOT NULL REFERENCES users(id),
    token           VARCHAR(255)    NOT NULL,
    device_id       VARCHAR(255)    NOT NULL,
    device_name     VARCHAR(100),
    platform        VARCHAR(10)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    version         INTEGER         NOT NULL DEFAULT 0,
    CONSTRAINT uq_push_tokens_token UNIQUE (token),
    CONSTRAINT uq_push_tokens_user_device UNIQUE (user_id, device_id)
);
CREATE INDEX idx_push_tokens_user_id ON push_tokens(user_id);
CREATE INDEX idx_push_tokens_active ON push_tokens(user_id, is_active) WHERE is_active = TRUE;
