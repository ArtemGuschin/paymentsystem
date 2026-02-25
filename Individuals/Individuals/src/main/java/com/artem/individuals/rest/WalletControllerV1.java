package com.artem.individuals.rest;

import com.artem.individuals.dto.request.CreateWalletRequestDto;
import com.artem.individuals.dto.response.WalletResponseDto;
import com.artem.individuals.service.WalletService;


import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletControllerV1 {

    private final WalletService walletService;

    @PostMapping
    public Mono<WalletResponseDto> create(@RequestBody CreateWalletRequestDto dto) {
        return walletService.create(dto);
    }

    @GetMapping("/{walletUid}")
    public Mono<WalletResponseDto> get(@PathVariable UUID walletUid) {
        return walletService.get(walletUid);
    }

    @GetMapping("/user/{userUid}")
    public List<WalletResponseDto> getByUser(@PathVariable UUID userUid) {
        return walletService.getByUser(userUid);
    }

    @DeleteMapping("/{walletUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID walletUid) {
        walletService.archive(walletUid);
    }
}
