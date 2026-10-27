
-- Общий тестовый тип кошелька для обоих шардов.
-- Flyway выполнит миграцию в каждой базе отдельно.

INSERT INTO wallet_types (
    uid,
    name,
    currency_code,
    status
)
VALUES (
           'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
           'Test USD',
           'USD',
           'ACTIVE'
       )
    ON CONFLICT (uid) DO NOTHING;
