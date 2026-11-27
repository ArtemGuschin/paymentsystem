CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,

                               event_id VARCHAR(36) NOT NULL UNIQUE,

                               created_at TIMESTAMP DEFAULT NOW() NOT NULL,
                               modified_at TIMESTAMP,

                               message TEXT NOT NULL,

                               user_uid VARCHAR(36) NOT NULL,

                               notification_status VARCHAR(32) NOT NULL DEFAULT 'NEW',

                               subject VARCHAR(128) NOT NULL,

                               created_by VARCHAR(255) NOT NULL,

                               notification_type VARCHAR(64) NOT NULL,

                               recipient_email VARCHAR(255)
);

CREATE INDEX idx_notifications_user_uid
    ON notifications (user_uid);

CREATE INDEX idx_notifications_status
    ON notifications (notification_status);