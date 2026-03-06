CREATE TABLE notification_log (
    id                VARCHAR(60)     PRIMARY KEY,
    user_id           VARCHAR(60)     NOT NULL REFERENCES users(id),
    group_id          VARCHAR(60)     REFERENCES groups(id),
    notification_type VARCHAR(50)     NOT NULL,
    title             VARCHAR(255)    NOT NULL,
    body              TEXT            NOT NULL,
    data_payload      JSONB,
    expo_ticket_id    VARCHAR(255),
    delivery_status   VARCHAR(20)     NOT NULL DEFAULT 'pending',
    failure_reason    TEXT,
    coalesce_key      VARCHAR(100),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at      TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_notification_log_user_id ON notification_log(user_id);
CREATE INDEX idx_notification_log_status ON notification_log(delivery_status) WHERE delivery_status IN ('pending', 'sent');
CREATE INDEX idx_notification_log_coalesce ON notification_log(user_id, coalesce_key, created_at);
CREATE INDEX idx_notification_log_ticket ON notification_log(expo_ticket_id) WHERE expo_ticket_id IS NOT NULL AND delivery_status = 'sent';
CREATE INDEX idx_notification_log_rate_limit ON notification_log(user_id, notification_type, created_at);
