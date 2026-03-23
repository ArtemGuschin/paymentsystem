TRUNCATE conversion_rates CASCADE;
TRUNCATE rate_providers CASCADE;

INSERT INTO rate_providers
(provider_code, created_at, modified_at, provider_name, description, priority, active)
VALUES
    ('CBR', now(), now(), 'Central Bank', 'Central bank rates', 1, true);

INSERT INTO conversion_rates
(created_at, source_code, destination_code, rate_begin_time, rate_end_time, rate, provider_code)
VALUES
    (now(), 'EUR', 'USD', '2020-01-01 00:00:00', '2030-01-01 00:00:00', 1.1, 'CBR');