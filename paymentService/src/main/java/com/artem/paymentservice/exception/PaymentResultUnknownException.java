package com.artem.paymentservice.exception;

public class PaymentResultUnknownException
        extends RuntimeException {

    public PaymentResultUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}