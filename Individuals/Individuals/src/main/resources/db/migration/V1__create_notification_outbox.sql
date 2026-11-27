
CREATE TABLE notification_outbox (
                                     id UUID PRIMARY KEY,
                                     event_id VARCHAR(36) NOT NULL UNIQUE,
                                     user_uid UUID NOT NULL,
                                     email VARCHAR(320) NOT NULL,
                                     notification_type VARCHAR(64) NOT NULL,
                                     message TEXT NOT NULL,
                                     subject VARCHAR(255) NOT NULL,
                                     created_by VARCHAR(64) NOT NULL,
                                     event_created_at TIMESTAMPTZ NOT NULL,

                                     status VARCHAR(20) NOT NULL DEFAULT 'NEW',
                                     attempt_count INTEGER NOT NULL DEFAULT 0,
                                     next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     last_error TEXT,
                                     published_at TIMESTAMPTZ,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT notification_outbox_status_check
                                         CHECK (status IN ('NEW', 'PUBLISHED', 'FAILED')),

                                     CONSTRAINT notification_outbox_attempt_count_check
                                         CHECK (attempt_count >= 0)
);

CREATE INDEX idx_notification_outbox_pending
    ON notification_outbox (status, next_attempt_at, created_at);
