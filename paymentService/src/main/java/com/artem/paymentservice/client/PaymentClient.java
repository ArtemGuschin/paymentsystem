package com.artem.paymentservice.client;

import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentClient {

    Flux<PaymentMethodResponse> getAvailablePaymentMethods(
            String currencyCode,
            String countryCode
    );

    Mono<PaymentResponse> processPayment(
            PaymentRequest request
    );
}