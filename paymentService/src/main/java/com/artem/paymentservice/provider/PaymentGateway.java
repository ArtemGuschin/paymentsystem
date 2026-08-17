package com.artem.paymentservice.provider;


import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;

public interface PaymentGateway {

    PaymentResponse processPayment(
            PaymentRequest request,
            String providerMethodType
    );

}