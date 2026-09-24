INSERT INTO merchants (
    merchant_id,
    secret_key,
    name,
    created_at
)
VALUES (
           'merchant_001',
           'payment-service-secret',
           'Default Service Merchant',
           CURRENT_TIMESTAMP
       )
    ON CONFLICT (merchant_id) DO NOTHING;