package com.artem.individuals.client;


import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service

public class PaymentClientImpl implements PaymentClient {

    private final WebClient paymentWebClient;

    public PaymentClientImpl(
            @Qualifier("paymentWebClient")
            WebClient paymentWebClient
    ) {
        this.paymentWebClient = paymentWebClient;
    }

    @Override
    public Mono<PaymentResponse> processPayment(
            PaymentRequest request
    ) {

        return paymentWebClient
                .post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class);
    }

    @Override
    public Flux<PaymentMethodResponse> getAvailablePaymentMethods(
            String currency,
            String country
    ) {

        return paymentWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/payment-methods/{currency}/{country}")
                        .build(currency, country))
                .retrieve()
                .bodyToFlux(PaymentMethodResponse.class);
    }
}