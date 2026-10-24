
ALTER TABLE notifications
    ADD COLUMN email_delivery_status VARCHAR(20)
        NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN email_attempt_count INTEGER
        NOT NULL DEFAULT 0,
    ADD COLUMN email_next_attempt_at TIMESTAMP,
    ADD COLUMN email_last_error TEXT;

-- Сохраняем сведения об уже отправленных письмах.
UPDATE notifications
SET email_delivery_status =
        CASE
            WHEN email_sent_at IS NOT NULL THEN 'SENT'
            ELSE 'PENDING'
            END,
    email_next_attempt_at =
        CASE
            WHEN email_sent_at IS NULL THEN CURRENT_TIMESTAMP
            ELSE NULL
            END
WHERE notification_type = 'USER_REGISTERED';

CREATE INDEX idx_notifications_email_delivery_due
    ON notifications (
                      email_delivery_status,
                      email_next_attempt_at
        );
