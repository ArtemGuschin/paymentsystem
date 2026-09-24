package com.artem.paymentservice.provider;

import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;

public interface PaymentGateway {

    PaymentResponse processPayment(
            PaymentRequest request,
            String providerMethodType
    );

    PaymentStatus getPaymentStatus(
            String providerTransactionId
    );
}