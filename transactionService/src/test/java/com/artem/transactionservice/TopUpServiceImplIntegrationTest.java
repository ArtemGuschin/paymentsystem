package com.artem.transactionservice;


import com.artem.transaction.model.TopUpCompleteRequest;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.WalletType;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.repository.WalletRepository;
import com.artem.transactionservice.repository.WalletTypeRepository;
import com.artem.transactionservice.service.TopUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TopUpServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TopUpService topUpService;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void completeTopUp_shouldBeIdempotent() {

        UUID userUid = UUID.randomUUID();

        /*
         * Создаём WalletType.
         */
        WalletType walletType = new WalletType();
        walletType.setName("TEST RUB");
        walletType.setCurrencyCode("RUB");
        walletType.setStatus("ACTIVE");
        walletType.setUserType("USER");

        /*
         * Здесь сохраняем WalletType напрямую через EntityManager,
         * чтобы не зависеть от WalletTypeService.
         */
        walletType = saveWalletType(walletType);

        /*
         * Создаём кошелёк с нулевым балансом.
         */
        Wallet wallet = new Wallet();
        wallet.setName("Test wallet");
        wallet.setUserUid(userUid);
        wallet.setWalletType(walletType);
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.saveAndFlush(wallet);

        /*
         * Создаём PENDING transaction.
         */
        Transaction transaction = new Transaction();
        transaction.setUserUid(userUid);
        transaction.setWallet(wallet);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.saveAndFlush(transaction);

        UUID transactionUid = transaction.getUid();

        TopUpCompleteRequest request =
                new TopUpCompleteRequest()
                        .providerTransactionId("provider-123");

        /*
         * Первый completeTopUp().
         */
        topUpService.completeTopUp(
                transactionUid,
                request
        );

        Wallet walletAfterFirstComplete =
                walletRepository.findByUid(wallet.getUid())
                        .orElseThrow();

        Transaction transactionAfterFirstComplete =
                transactionRepository.findById(transactionUid)
                        .orElseThrow();

        assertEquals(
                BigDecimal.valueOf(100),
                walletAfterFirstComplete.getBalance()
        );

        assertEquals(
                "COMPLETED",
                transactionAfterFirstComplete.getStatus()
        );

        assertEquals(
                "provider-123",
                transactionAfterFirstComplete
                        .getProviderTransactionId()
        );

        /*
         * Второй вызов с тем же transactionUid.
         *
         * Это и есть проверка идемпотентности.
         */
        topUpService.completeTopUp(
                transactionUid,
                request
        );

        Wallet walletAfterSecondComplete =
                walletRepository.findByUid(wallet.getUid())
                        .orElseThrow();

        Transaction transactionAfterSecondComplete =
                transactionRepository.findById(transactionUid)
                        .orElseThrow();

        /*
         * Баланс НЕ должен стать 200.
         */
        assertEquals(
                BigDecimal.valueOf(100),
                walletAfterSecondComplete.getBalance()
        );

        assertEquals(
                "COMPLETED",
                transactionAfterSecondComplete.getStatus()
        );

        assertEquals(
                "provider-123",
                transactionAfterSecondComplete
                        .getProviderTransactionId()
        );

        assertNotNull(
                transactionAfterSecondComplete.getUid()
        );
    }

    private WalletType saveWalletType(
            WalletType walletType
    ) {
        return walletTypeRepository
                .saveAndFlush(walletType);
    }

    
}