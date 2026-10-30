-- 1. Добавляем признак основного события.
-- Все новые события по умолчанию считаются основными.

ALTER TABLE notification_outbox
    ADD COLUMN IF NOT EXISTS registration_canonical
    BOOLEAN NOT NULL DEFAULT TRUE;


-- 2. Для исторических USER_REGISTERED
-- оставляем основным только самое раннее событие.

WITH ranked_events AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY user_uid
            ORDER BY created_at ASC, id ASC
        ) AS row_num
    FROM notification_outbox
    WHERE notification_type = 'USER_REGISTERED'
)
UPDATE notification_outbox AS n
SET registration_canonical = FALSE
    FROM ranked_events AS r
WHERE n.id = r.id
  AND r.row_num > 1;


-- 3. Запрещаем создавать второе основное событие
-- USER_REGISTERED для одного пользователя.

CREATE UNIQUE INDEX IF NOT EXISTS
    ux_notification_outbox_user_registered
    ON notification_outbox (user_uid)
    WHERE notification_type = 'USER_REGISTERED'
    AND registration_canonical = TRUE;