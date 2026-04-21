-- =====================================================
-- Тестовые данные для fake-payment-provider
-- =====================================================

-- 1. Мерчанты (secret_key закодирован BCrypt, пароль = 'test_secret')
INSERT INTO merchants (merchant_id, secret_key, name, created_at) VALUES
                                                                      ('merchant_001', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.cZ4lH9YgQpG8qQz9F3LqZ5JkXCe', 'Test Merchant 1', CURRENT_TIMESTAMP),
                                                                      ('merchant_002', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.cZ4lH9YgQpG8qQz9F3LqZ5JkXCe', 'Test Merchant 2', CURRENT_TIMESTAMP),
                                                                      ('merchant_003', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.cZ4lH9YgQpG8qQz9F3LqZ5JkXCe', 'Test Merchant 3', CURRENT_TIMESTAMP);

-- 2. Транзакции
INSERT INTO transactions (merchant_id, amount, currency, method, status, created_at, updated_at, description, external_id, notification_url) VALUES
                                                                                                                                                 (1, 100.50, 'USD', 'CARD', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 'Payment for order #1001', 'ext_1001', 'https://merchant1.com/webhook'),
                                                                                                                                                 (1, 25.00, 'EUR', 'PAYPAL', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'Subscription payment', 'ext_1002', 'https://merchant1.com/webhook'),
                                                                                                                                                 (2, 500.00, 'GBP', 'CARD', 'FAILED', CURRENT_TIMESTAMP, NULL, 'Failed transaction', 'ext_2001', 'https://merchant2.com/notify'),
                                                                                                                                                 (3, 10.99, 'USD', 'CARD', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours', 'Tip', 'ext_3001', 'https://merchant3.com/hook');

-- 3. Выплаты
INSERT INTO payouts (merchant_id, amount, currency, status, created_at, updated_at, external_id, notification_url) VALUES
                                                                                                                       (1, 200.00, 'USD', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'payout_1001', 'https://merchant1.com/payout'),
                                                                                                                       (2, 75.50, 'EUR', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'payout_2001', 'https://merchant2.com/payout'),
                                                                                                                       (3, 1000.00, 'JPY', 'FAILED', CURRENT_TIMESTAMP, NULL, 'payout_3001', 'https://merchant3.com/payout');

-- 4. Вебхуки (связаны с транзакциями/выплатами)
INSERT INTO webhooks (event_type, entity_id, payload, received_at, notification_url) VALUES
                                                                                         ('TRANSACTION_SUCCESS', 1, '{"reason": "payment completed"}', CURRENT_TIMESTAMP - INTERVAL '2 days', 'https://merchant1.com/webhook'),
                                                                                         ('TRANSACTION_PENDING', 2, '{"waiting": "capture"}', CURRENT_TIMESTAMP - INTERVAL '1 day', 'https://merchant1.com/webhook'),
                                                                                         ('TRANSACTION_FAILED', 3, '{"error": "insufficient funds"}', CURRENT_TIMESTAMP, 'https://merchant2.com/notify'),
                                                                                         ('PAYOUT_SUCCESS', 1, '{"batch": "batch_001"}', CURRENT_TIMESTAMP - INTERVAL '5 days', 'https://merchant1.com/payout'),
                                                                                         ('PAYOUT_PENDING', 2, '{}', CURRENT_TIMESTAMP - INTERVAL '1 day', 'https://merchant2.com/payout'),
                                                                                         ('PAYOUT_FAILED', 3, '{"code": "500"}', CURRENT_TIMESTAMP, 'https://merchant3.com/payout');