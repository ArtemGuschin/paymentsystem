DELETE FROM transactions;
DELETE FROM merchants;

INSERT INTO merchants (id, merchant_id, secret_key)
VALUES (1, 'merchant_001', '{noop}test_secret');