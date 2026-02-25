package com.artem.transactionservice.controller;

import com.artem.transaction.model.CreateWalletRequest;
import com.artem.transaction.model.UpdateWalletRequest;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletControllerV1 {

    private final WalletService walletService;

    @PostMapping
    @Transactional
    public Wallet createWallet(@RequestBody CreateWalletRequest createWalletRequest) {
        return walletService.createWallet(createWalletRequest);

    }

    @PatchMapping("/{walletUid}")
    public Wallet updateWallet(
            @PathVariable UUID walletUid,
            @RequestBody UpdateWalletRequest updateWalletRequest
    ) {
        return walletService.updateWallet(walletUid, updateWalletRequest);
    }

    @GetMapping("/{user_uid}")
    public List<Wallet> getWalletByUserUid(@PathVariable("user_uid") UUID user_uid) {
        return walletService.findAllWalletsByUserId(user_uid);
    }

    @GetMapping("/{userId}/currency/{currency}")
    public List<Wallet> getAllWalletsByUserIdAndCurrency(@PathVariable("userId") UUID userId, @PathVariable String currency) {
        return walletService.findAllWalletsByUserIdAndWalletTypeCurrencyCode(userId, currency);

    }

}
