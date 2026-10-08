package com.artem.paymentservice.provider;

import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import java.util.function.Consumer;

public interface PaymentGateway {

    PaymentResponse processPayment(
            PaymentRequest request,
            String providerMethodType,
            Consumer<String> providerTransactionIdConsumer
    );

    PaymentStatus getPaymentStatus(
            String providerTransactionId
    );
}