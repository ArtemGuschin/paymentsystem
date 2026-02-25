package com.artem.individuals.service;


import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.request.TopUpInitRequestDto;
import com.artem.individuals.dto.response.TopUpInitResponseDto;
import com.artem.individuals.dto.response.TopUpConfirmResponseDto;

import com.artem.transaction.client.api.TopUpApi;
import com.artem.transaction.client.model.TopUpInitRequest;
import com.artem.transaction.client.model.TopUpInitResponse;
import com.artem.transaction.client.model.TopUpConfirmRequest;
import com.artem.transaction.client.model.TopUpConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TopUpService {

    private final TopUpApi topUpApi;


    public Mono<TopUpInitResponseDto> init(TopUpInitRequestDto dto) {

        TopUpInitRequest request = new TopUpInitRequest()
                .userUid(dto.getUserUid())
                .walletUid(dto.getWalletUid())
                .amount(dto.getAmount());


        return Mono.fromCallable(() -> topUpApi.initTopUp(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return TopUpInitResponseDto.builder()
                            .available(response.getAvailable())
                            .fee(response.getFee())
                            .totalAmount(response.getTotalAmount())
                            .currency(response.getCurrency())
                            .message(response.getMessage())
                            .build();
                });


    }


    public Mono<TopUpConfirmResponseDto> confirm(TopUpConfirmRequestDto dto) {

        TopUpConfirmRequest request = new TopUpConfirmRequest()
                .userUid(dto.getUserUid())
                .walletUid(dto.getWalletUid())
                .amount(dto.getAmount())
                .comment(dto.getComment());

        return Mono.fromCallable(() -> topUpApi.confirmTopUp(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return TopUpConfirmResponseDto.builder()
                            .transactionUuid(response.getTransactionUid())
                            .status(response.getStatus())
                            .build();
                });


    }
}

