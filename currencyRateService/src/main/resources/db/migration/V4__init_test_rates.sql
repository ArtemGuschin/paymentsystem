INSERT INTO conversion_rates
(source_code, destination_code, rate_begin_time, rate_end_time, rate, provider_code)
VALUES
    ('USD', 'RUB', now(), '2099-12-31', 92.00, 'ECB'),
    ('EUR', 'RUB', now(), '2099-12-31', 100.00, 'ECB'),
    ('EUR', 'USD', now(), '2099-12-31', 1.08, 'ECB');