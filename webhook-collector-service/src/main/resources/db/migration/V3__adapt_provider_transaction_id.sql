ALTER TABLE payment_provider_callbacks
DROP CONSTRAINT uq_payment_provider_callback;

ALTER TABLE payment_provider_callbacks
DROP COLUMN provider_transaction_uid;

ALTER TABLE payment_provider_callbacks
    ADD COLUMN provider_transaction_id BIGINT NOT NULL;

ALTER TABLE payment_provider_callbacks
    ADD CONSTRAINT uq_payment_provider_callback
        UNIQUE (provider, provider_transaction_id, type);