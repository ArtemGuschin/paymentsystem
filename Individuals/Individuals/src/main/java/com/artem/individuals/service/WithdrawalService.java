package com.artem.individuals.service;

import com.artem.individuals.dto.request.WithdrawalConfirmRequestDto;
import com.artem.individuals.dto.request.WithdrawalInitRequestDto;
import com.artem.individuals.dto.response.WithdrawalConfirmResponseDto;
import com.artem.individuals.dto.response.WithdrawalInitResponseDto;
import com.artem.transaction.client.api.WithdrawalApi;
import com.artem.transaction.client.model.WithdrawalConfirmRequest;
import com.artem.transaction.client.model.WithdrawalConfirmResponse;
import com.artem.transaction.client.model.WithdrawalInitRequest;
import com.artem.transaction.client.model.WithdrawalInitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class WithdrawalService {
    private final WithdrawalApi withdrawalApi;

    public Mono<WithdrawalInitResponseDto> init(WithdrawalInitRequestDto requestDto) {
        WithdrawalInitRequest request = new WithdrawalInitRequest()
                .userUid(requestDto.getUserUid())
                .walletUid(requestDto.getWalletUid())
                .amount(requestDto.getAmount());

        return Mono.fromCallable(() -> withdrawalApi.initWithdrawal(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return WithdrawalInitResponseDto.builder()
                            .available(response.getAvailable())
                            .fee(response.getFee())
                            .totalAmount(response.getTotalAmount())
                            .currency(response.getCurrency())
                            .message(response.getMessage())
                            .build();

                });


    }

    public Mono<WithdrawalConfirmResponseDto> confirm(WithdrawalConfirmRequestDto requestDto) {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest()
                .userUid(requestDto.getUserUid())
                .walletUid(requestDto.getWalletUid())
                .amount(requestDto.getAmount());
        return Mono.fromCallable(() -> withdrawalApi.confirmWithdrawal(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return WithdrawalConfirmResponseDto.builder()
                            .transactionUid(response.getTransactionUid())
                            .status(response.getStatus())
                            .build();
                });


    }
}
