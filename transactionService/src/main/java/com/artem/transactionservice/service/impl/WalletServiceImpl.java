package com.artem.transactionservice.service.impl;

import com.artem.transaction.model.CreateWalletRequest;
import com.artem.transaction.model.UpdateWalletRequest;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.WalletType;
import com.artem.transactionservice.repository.WalletRepository;
import com.artem.transactionservice.repository.WalletTypeRepository;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTypeRepository walletTypeRepository;

    @Override
    public Wallet createWallet(CreateWalletRequest dto) {

        WalletType walletType = walletTypeRepository
                .findById(dto.getWalletTypeUid())
                .orElseThrow(() -> new IllegalArgumentException("Wallet type not found"));

        Wallet wallet = new Wallet();
        wallet.setUid(UUID.randomUUID());
        wallet.setUserUid(dto.getUserUid());
        wallet.setName(dto.getName());
        wallet.setWalletType(walletType);
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

         return walletRepository.save(wallet);
    }

    @Override
    public Wallet updateWallet(UUID walletId, UpdateWalletRequest dto) {
        Wallet wallet = walletRepository.findByUid(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (dto.getName() != null) {
            wallet.setName(dto.getName());
        }

        if (dto.getStatus() != null) {
            wallet.setStatus(dto.getStatus().getValue());
        }

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet getWalletForUpdate(UUID walletUid, UUID userUid) {
        return walletRepository
                .findByUidAndUserUidForUpdate(walletUid, userUid)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Override
    public List<Wallet> findAllWalletsByUserId(UUID id) {
        return walletRepository.findAllByUserUid(id);
    }

    @Override
    public List<Wallet> findAllWalletsByUserIdAndWalletTypeCurrencyCode(UUID id, String currencyCode) {
        return walletRepository.findAllByUserUidAndWalletType_CurrencyCode(id, currencyCode);
    }

    @Override
    public Wallet getActiveWallet(UUID walletUid, UUID userUid) {
        if (walletUid == null || userUid == null) {
            throw new IllegalArgumentException("walletUid and userUid must not be null");
        }

        return walletRepository
                .findByUidAndUserUidAndStatus(walletUid, userUid, "ACTIVE")
                .orElseThrow(() ->
                        new RuntimeException("Active wallet not found")
                );
    }

    @Transactional
    @Override
    public void increaseBalance(UUID walletUid, UUID userUid, BigDecimal amount) {
        Wallet wallet = walletRepository
                .findByUidAndUserUidAndStatus(walletUid, userUid, "ACTIVE")
                .orElseThrow();

        wallet.setBalance(wallet.getBalance().add(amount));
    }

    @Transactional
    @Override
    public void decreaseBalance(UUID walletUid, UUID userUid, BigDecimal amount) {

        Wallet wallet = walletRepository
                .findByUidAndUserUidAndStatus(walletUid, userUid, "ACTIVE")
                .orElseThrow();

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
    }
}
