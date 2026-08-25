package com.artem.individuals.service;

import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.response.PaymentResponseDto;
import com.artem.individuals.dto.response.TopUpResultResponseDto;
import com.artem.individuals.exception.TopUpOrchestrationException;
import com.artem.transaction.client.api.TopUpApi;
import com.artem.transaction.client.model.TopUpCompleteRequest;
import com.artem.transaction.client.model.TopUpFailRequest;
import com.artem.transaction.client.model.TopUpResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopUpOrchestrationService {

    private final TopUpService topUpService;
    private final PaymentService paymentService;
    private final TopUpApi topUpApi;

    public Mono<TopUpResultResponseDto> confirmTopUp(
            TopUpConfirmRequestDto dto
    ) {

        log.info(
                "Start top up confirmation. userUid={}, walletUid={}, amount={}",
                dto.getUserUid(),
                dto.getWalletUid(),
                dto.getAmount()
        );

        return topUpService.confirm(dto)
                .flatMap(confirmResponse -> {

                    log.info(
                            "Transaction created. transactionUid={}, status={}",
                            confirmResponse.getTransactionUuid(),
                            confirmResponse.getStatus()
                    );

                    PaymentRequestDto paymentRequest =
                            PaymentRequestDto.builder()
                                    .internalTransactionUid(
                                            confirmResponse.getTransactionUuid()
                                    )
                                    .methodId(dto.getPaymentMethodId())
                                    .amount(dto.getAmount().doubleValue())
                                    .currency(dto.getCurrency())
                                    .userFields(dto.getPaymentFields())
                                    .build();

                    return paymentService.processPayment(paymentRequest)
                            .flatMap(paymentResponse -> {

                                log.info(
                                        "Payment response received. transactionUid={}, providerTransactionId={}, status={}",
                                        confirmResponse.getTransactionUuid(),
                                        paymentResponse.getProviderTransactionId(),
                                        paymentResponse.getStatus()
                                );

                                return finalizeTopUp(
                                        confirmResponse.getTransactionUuid(),
                                        paymentResponse
                                );
                            })
                            .onErrorMap(ex -> {

                                if (ex instanceof TopUpOrchestrationException) {
                                    return ex;
                                }

                                log.error(
                                        "Top up orchestration failed. transactionUid={}",
                                        confirmResponse.getTransactionUuid(),
                                        ex
                                );

                                return new TopUpOrchestrationException(
                                        "Top up confirmation failed",
                                        ex
                                );
                            });
                });
    }

    private Mono<TopUpResultResponseDto> finalizeTopUp(
            java.util.UUID transactionUid,
            PaymentResponseDto payment
    ) {

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {

            TopUpCompleteRequest request =
                    new TopUpCompleteRequest()
                            .providerTransactionId(
                                    payment.getProviderTransactionId()
                            );

            return Mono.fromCallable(() ->
                            topUpApi.completeTopUp(
                                    transactionUid,
                                    request
                            ))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(this::toTopUpResultResponseDto);
        }

        if ("FAILED".equalsIgnoreCase(payment.getStatus())) {

            TopUpFailRequest request =
                    new TopUpFailRequest();

            return Mono.fromCallable(() ->
                            topUpApi.failTopUp(
                                    transactionUid,
                                    request
                            ))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(result ->
                            toTopUpResultResponseDto(
                                    result,
                                    payment.getStatus()
                            ));
        }

        return Mono.error(
                new TopUpOrchestrationException(
                        "Unsupported payment status: "
                                + payment.getStatus()
                )
        );
    }

    private TopUpResultResponseDto toTopUpResultResponseDto(
            TopUpResultResponse result
    ) {

        return TopUpResultResponseDto.builder()
                .transactionUuid(
                        result.getTransactionUid()
                )
                .transactionStatus(
                        result.getStatus() != null
                                ? result.getStatus().getValue()
                                : null
                )
                .providerTransactionId(
                        result.getProviderTransactionId()
                )
                .paymentStatus(
                        result.getStatus() != null
                                && "COMPLETED".equals(
                                result.getStatus().getValue()
                        )
                                ? "SUCCESS"
                                : "FAILED"
                )
                .build();
    }

    private TopUpResultResponseDto toTopUpResultResponseDto(
            TopUpResultResponse result,
            String paymentStatus
    ) {

        return TopUpResultResponseDto.builder()
                .transactionUuid(
                        result.getTransactionUid()
                )
                .transactionStatus(
                        result.getStatus() != null
                                ? result.getStatus().getValue()
                                : null
                )
                .providerTransactionId(
                        result.getProviderTransactionId()
                )
                .paymentStatus(paymentStatus)
                .build();
    }
}