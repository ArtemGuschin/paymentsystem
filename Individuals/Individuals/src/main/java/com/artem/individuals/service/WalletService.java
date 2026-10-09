package com.artem.individuals.service;

import com.artem.individuals.dto.request.CreateWalletRequestDto;
import com.artem.individuals.dto.response.WalletResponseDto;

import com.artem.transaction.client.api.WalletsApi;
import com.artem.transaction.client.model.CreateWalletRequest;
import com.artem.transaction.client.model.WalletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletsApi walletsApi;

    public Mono<WalletResponseDto> create(CreateWalletRequestDto dto) {

        CreateWalletRequest request = new CreateWalletRequest()
                .userUid(dto.getUserUid())
                .name(dto.getName())
                .walletTypeUid(dto.getWalletTypeUid());


        return Mono.fromCallable(() -> walletsApi.createWallet(request))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(this::map);


    }

    public Mono<WalletResponseDto> get(UUID walletUid) {
        return Mono.fromCallable(() -> walletsApi.getWalletByUid(walletUid))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(this::map);

    }

    public List<WalletResponseDto> getByUser(UUID userUid) {
        return walletsApi.getUserWallets(userUid)
                .stream()
                .map(this::map)
                .toList();
    }

    public void archive(UUID walletUid) {
        walletsApi.archiveWallet(walletUid);
    }


    private WalletResponseDto map(WalletResponse response) {
        return WalletResponseDto.builder()
                .walletUid(response.getUid())
                .name(response.getName())
                .userUid(UUID.fromString(response.getUserUid()))
                .walletTypeUid(
                        response.getWalletType() != null
                                ? response.getWalletType().getUid()
                                : null
                )
                .walletTypeName(response.getWalletType().getName())
                .walletTypeCurrencyCode(response.getWalletType().getCurrencyCode())
                .balance(response.getBalance())
                .status(response.getStatus())
                .build();
    }
}
