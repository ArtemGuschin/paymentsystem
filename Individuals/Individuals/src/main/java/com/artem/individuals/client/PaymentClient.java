package com.artem.individuals.client;

import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentClient {

    Mono<PaymentResponse> processPayment(
            PaymentRequest request
    );

    Flux<PaymentMethodResponse> getAvailablePaymentMethods(
            String currency,
            String country
    );
}