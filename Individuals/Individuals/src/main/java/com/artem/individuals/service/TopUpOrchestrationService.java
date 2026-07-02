package com.artem.individuals.service;


import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.response.PaymentResponseDto;
import com.artem.individuals.dto.response.TopUpConfirmResponseDto;
import com.artem.individuals.dto.response.TopUpResultResponseDto;
import com.artem.individuals.exception.TopUpOrchestrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopUpOrchestrationService {

    private final TopUpService topUpService;
    private final PaymentService paymentService;

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
                            "Transaction confirmed. transactionUid={}, status={}",
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
                            .doOnSuccess(paymentResponse ->
                                    log.info(
                                            "Payment completed. providerTransactionId={}",
                                            paymentResponse.getProviderTransactionId()
                                    )).doOnError(ex ->
                                    log.error(
                                            "Payment processing failed. transactionUid={}",
                                            confirmResponse.getTransactionUuid(),
                                            ex
                                    ))
                            .onErrorMap(ex ->
                                    new TopUpOrchestrationException(
                                            "Top up confirmation failed",
                                            ex
                                    ))

                            .map(paymentResponse ->
                                    buildResult(
                                            confirmResponse,
                                            paymentResponse
                                    ));
                });
    }

    private TopUpResultResponseDto buildResult(
            TopUpConfirmResponseDto transaction,
            PaymentResponseDto payment
    ) {

        return TopUpResultResponseDto.builder()
                .transactionUuid(transaction.getTransactionUuid())
                .transactionStatus(transaction.getStatus())
                .providerTransactionId(payment.getProviderTransactionId())
                // TODO получить реальный статус из PaymentService
                .paymentStatus("SUCCESS")
                .build();
    }
}