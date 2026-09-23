DROP INDEX IF EXISTS idx_payment_internal_tx;

ALTER TABLE payments
    ALTER COLUMN internal_transaction_id SET NOT NULL;

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_internal_transaction_id
        UNIQUE (internal_transaction_id);