package com.artem.transactionservice.integration;


import com.artem.transactionservice.TestcontainersConfiguration;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.WalletType;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.repository.WalletRepository;
import com.artem.transactionservice.repository.WalletTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PaymentStatusUpdatedConsumerIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        walletTypeRepository.deleteAll();
    }

    @Test
    void shouldUpdateTransactionStatusFromPendingToSuccess() throws Exception {

        // 1. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 2. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Test wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        // 3. Создаём PENDING-транзакцию
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        // 4. Формируем то же событие, которое приходит из Webhook Collector
        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 5. Публикуем его в настоящую Kafka из Testcontainers
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // 6. Consumer работает асинхронно,
        // поэтому ждём, пока он изменит запись в PostgreSQL
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Transaction updated =
                            transactionRepository.findById(transactionUid)
                                    .orElseThrow();

                    assertThat(updated.getStatus())
                            .isEqualTo("SUCCESS");

                    assertThat(updated.getModifiedAt())
                            .isNotNull();
                });
    }
}