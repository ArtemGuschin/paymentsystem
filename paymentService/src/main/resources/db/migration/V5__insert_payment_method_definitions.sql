-- Разрешаем CARD для всех валют
INSERT INTO payment_method_definitions (
    payment_method_id,
    is_active,
    is_all_currencies,
    currency_code
)
SELECT
    id,
    true,
    true,
    NULL
FROM payment_methods
WHERE type = 'CARD'
  AND NOT EXISTS (
    SELECT 1
    FROM payment_method_definitions d
    WHERE d.payment_method_id = payment_methods.id
);

-- Разрешаем SBP для всех валют
INSERT INTO payment_method_definitions (
    payment_method_id,
    is_active,
    is_all_currencies,
    currency_code
)
SELECT
    id,
    true,
    true,
    NULL
FROM payment_methods
WHERE type = 'SBP'
  AND NOT EXISTS (
    SELECT 1
    FROM payment_method_definitions d
    WHERE d.payment_method_id = payment_methods.id
);