package com.artem.paymentservice.exception;

public class PaymentMethodNotFoundException extends RuntimeException {
    public PaymentMethodNotFoundException(Long id) {
        super("Payment method with id=" + id + " not found");
    }
}
