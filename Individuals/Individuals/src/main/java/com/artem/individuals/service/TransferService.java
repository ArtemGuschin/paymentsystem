package com.artem.individuals.service;

import com.artem.individuals.dto.request.TransferConfirmRequestDto;
import com.artem.individuals.dto.request.TransferInitRequestDto;
import com.artem.individuals.dto.response.TransferConfirmResponseDto;
import com.artem.individuals.dto.response.TransferInitResponseDto;
import com.artem.transaction.client.api.TransferApi;
import com.artem.transaction.client.model.TransferConfirmRequest;
import com.artem.transaction.client.model.TransferConfirmResponse;
import com.artem.transaction.client.model.TransferInitRequest;
import com.artem.transaction.client.model.TransferInitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferApi transferApi;

    public Mono<TransferInitResponseDto> init(TransferInitRequestDto requestDto) {

        TransferInitRequest request = new TransferInitRequest()
                .userUid(requestDto.getUserUid())
                .walletUid(requestDto.getWalletUid())
                .targetWalletUid(requestDto.getTargetWalletUid())
                .amount(requestDto.getAmount());

        return Mono.fromCallable(() -> transferApi.initTransfer(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return TransferInitResponseDto.builder()
                            .available(response.getAvailable())
                            .fee(response.getFee())
                            .totalAmount(response.getTotalAmount())
                            .currency(response.getCurrency())
                            .message(response.getMessage())
                            .build();
                });


    }

    public Mono<TransferConfirmResponseDto> confirm(TransferConfirmRequestDto requestDto) {
        TransferConfirmRequest request = new TransferConfirmRequest()
                .userUid(requestDto.getUserUid())
                .walletUid(requestDto.getWalletUid())
                .targetWalletUid(requestDto.getTargetWalletUid())
                .amount(requestDto.getAmount());

        return Mono.fromCallable(() -> transferApi.confirmTransfer(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(response -> {
                    return TransferConfirmResponseDto.builder()
                            .transactionUid(response.getTransactionUid())
                            .status(response.getStatus())
                            .build();
                });


    }
}
