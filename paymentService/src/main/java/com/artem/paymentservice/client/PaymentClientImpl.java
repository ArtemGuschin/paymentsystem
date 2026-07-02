package com.artem.paymentservice.client;

import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.exception.PaymentClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class PaymentClientImpl implements PaymentClient {

    private final WebClient webClient;

    @Override
    public Flux<PaymentMethodResponse> getAvailablePaymentMethods(
            String currencyCode,
            String countryCode
    ) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/payment-methods/{currencyCode}/{countryCode}")
                        .build(currencyCode, countryCode))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()

                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new PaymentClientException(
                                                "PaymentService returned 4xx: " + body))))

                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new PaymentClientException(
                                                "PaymentService returned 5xx: " + body))))

                .bodyToFlux(PaymentMethodResponse.class)

                .doOnSubscribe(subscription ->
                        log.info("Loading payment methods. currency={}, country={}",
                                currencyCode,
                                countryCode))

                .doOnComplete(() ->
                        log.info("Payment methods successfully loaded"))

                .doOnError(ex ->
                        log.error("Cannot load payment methods", ex));
    }

    @Override
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {

        return webClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()

                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new PaymentClientException(
                                                "PaymentService returned 4xx: " + body))))

                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new PaymentClientException(
                                                "PaymentService returned 5xx: " + body))))

                .bodyToMono(PaymentResponse.class)

                .doOnSubscribe(subscription ->
                        log.info("Processing payment. transactionUid={}",
                                request.getInternalTransactionUid()))

                .doOnSuccess(response ->
                        log.info("Payment processed successfully. providerTransactionId={}",
                                response.getProviderTransactionId()))

                .doOnError(ex ->
                        log.error("Payment processing failed", ex));
    }
}