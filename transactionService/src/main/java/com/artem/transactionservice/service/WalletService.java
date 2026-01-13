package com.artem.transactionservice.service;

import com.artem.transaction.model.CreateWalletRequest;
import com.artem.transaction.model.UpdateWalletRequest;
import com.artem.transactionservice.entity.Wallet;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    @Transactional
    Wallet createWallet(CreateWalletRequest dto);


    Wallet updateWallet(UUID walletId, UpdateWalletRequest dto);
    Wallet getWalletForUpdate(UUID walletUid, UUID userUid);

    List<Wallet> findAllWalletsByUserId(UUID id);

    List<Wallet> findAllWalletsByUserIdAndWalletTypeCurrencyCode(UUID id, String currencyCode);

    Wallet getActiveWallet(UUID walletUid, UUID userUid);



    @Transactional
    void increaseBalance(UUID walletUid, UUID userUid, BigDecimal amount);
    @Transactional
    void decreaseBalance(UUID walletUid, UUID userUid, BigDecimal amount);
}

