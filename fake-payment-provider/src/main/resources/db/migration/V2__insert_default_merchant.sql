INSERT INTO merchants (
    merchant_id,
    secret_key,
    name
)
VALUES (
           'merchant_001',
           'secret_001',
           'Default Merchant'
       )
    ON CONFLICT (merchant_id) DO NOTHING;