package com.artem.paymentservice.exception;

public class PaymentIdempotencyConflictException
        extends RuntimeException {

    public PaymentIdempotencyConflictException(
            String internalTransactionId
    ) {

        super(
                "Payment with internalTransactionUid="
                        + internalTransactionId
                        + " already exists with different request parameters"
        );
    }
}