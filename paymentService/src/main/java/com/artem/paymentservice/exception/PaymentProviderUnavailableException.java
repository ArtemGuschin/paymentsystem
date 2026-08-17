package com.artem.paymentservice.exception;

public class PaymentProviderUnavailableException extends RuntimeException {

    public PaymentProviderUnavailableException(Throwable cause) {
        super("Payment provider is temporarily unavailable", cause);
    }
}