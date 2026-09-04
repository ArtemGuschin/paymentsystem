-- Создаем провайдера, если его еще нет
INSERT INTO payment_providers (name, description)
VALUES ('FAKE', 'Training provider')
    ON CONFLICT DO NOTHING;

-- CARD
INSERT INTO payment_methods (
    provider_id,
    type,
    name,
    is_active,
    provider_unique_id,
    provider_method_type,
    profile_type,
    modified_at
)
SELECT
    id,
    'CARD',
    'Bank Card',
    true,
    'CARD',
    'CARD',
    'INDIVIDUAL',
    NOW()
FROM payment_providers
WHERE name = 'FAKE'
  AND NOT EXISTS (
    SELECT 1
    FROM payment_methods
    WHERE type = 'CARD'
      AND provider_id = payment_providers.id
);

-- SBP
INSERT INTO payment_methods (
    provider_id,
    type,
    name,
    is_active,
    provider_unique_id,
    provider_method_type,
    profile_type,
    modified_at
)
SELECT
    id,
    'SBP',
    'Fast Payments',
    true,
    'SBP',
    'SBP',
    'INDIVIDUAL',
    NOW()
FROM payment_providers
WHERE name = 'FAKE'
  AND NOT EXISTS (
    SELECT 1
    FROM payment_methods
    WHERE type = 'SBP'
      AND provider_id = payment_providers.id
);