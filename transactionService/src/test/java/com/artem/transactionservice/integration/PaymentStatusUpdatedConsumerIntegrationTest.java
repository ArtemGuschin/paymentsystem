package com.artem.transactionservice.integration;


import com.artem.transactionservice.TestcontainersConfiguration;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.WalletType;
import com.artem.transactionservice.kafka.PaymentStatusUpdatedConsumer;
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
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PaymentStatusUpdatedConsumerIntegrationTest {

    @Autowired
    private PaymentStatusUpdatedConsumer paymentStatusUpdatedConsumer;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private KafkaContainer kafkaContainer;

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

        // 2. Создаём кошелёк с нулевым балансом
        Wallet wallet = new Wallet();
        wallet.setName("Test wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        UUID walletUid = wallet.getUid();

        // 3. Создаём PENDING DEPOSIT-транзакцию
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        // 4. Формируем событие SUCCESS от Webhook Collector
        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 5. Отправляем событие в Kafka
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // 6. Ждём, пока consumer:
        //    - переведёт transaction в SUCCESS
        //    - зачислит деньги на wallet
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Transaction updatedTransaction =
                            transactionRepository.findById(transactionUid)
                                    .orElseThrow();

                    assertThat(updatedTransaction.getStatus())
                            .isEqualTo("SUCCESS");

                    assertThat(updatedTransaction.getModifiedAt())
                            .isNotNull();

                    Wallet updatedWallet =
                            walletRepository.findById(walletUid)
                                    .orElseThrow();

                    assertThat(updatedWallet.getBalance())
                            .isEqualByComparingTo("1000.00");
                });
    }

    @Test
    void shouldConsumeEventPublishedBeforeListenerIsReady() throws Exception {

        MessageListenerContainer listener =
                kafkaListenerEndpointRegistry
                        .getListenerContainer("paymentStatusUpdatedListener");

        assertThat(listener).isNotNull();

        // 1. Останавливаем consumer
        listener.stop();

        // 2. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_EARLIEST_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 3. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Earliest test wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        // 4. Создаём PENDING-транзакцию
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 5. Consumer остановлен.
        // Событие сначала физически попадает в Kafka.
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // В этот момент transaction всё ещё должна быть PENDING.
        Transaction beforeConsumerStart =
                transactionRepository.findById(transactionUid)
                        .orElseThrow();

        assertThat(beforeConsumerStart.getStatus())
                .isEqualTo("PENDING");

        // 6. Только теперь запускаем consumer.
        listener.start();

        // 7. Consumer без сохранённого offset должен прочитать
        // уже существующее сообщение благодаря earliest.
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

    @Test
    void shouldMarkTransactionFailedWithoutChangingBalance() throws Exception {

        // 1. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_FAILED_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 2. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Failed payment wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        UUID walletUid = wallet.getUid();

        // 3. Создаём PENDING DEPOSIT-транзакцию
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        // 4. Формируем FAILED событие
        String message = """
                {
                  "transactionUid": "%s",
                  "status": "FAILED"
                }
                """.formatted(transactionUid);

        // 5. Отправляем событие в Kafka
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // 6. Проверяем:
        //    - transaction = FAILED
        //    - balance остался 0
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Transaction updatedTransaction =
                            transactionRepository.findById(transactionUid)
                                    .orElseThrow();

                    assertThat(updatedTransaction.getStatus())
                            .isEqualTo("FAILED");

                    assertThat(updatedTransaction.getModifiedAt())
                            .isNotNull();

                    Wallet updatedWallet =
                            walletRepository.findById(walletUid)
                                    .orElseThrow();

                    assertThat(updatedWallet.getBalance())
                            .isEqualByComparingTo("0.00");
                });
    }

    @Test
    void shouldNotIncreaseBalanceTwiceForDuplicateSuccessEvent() throws Exception {

        // 1. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_DUPLICATE_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 2. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Duplicate success wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        UUID walletUid = wallet.getUid();

        // 3. Создаём PENDING DEPOSIT-транзакцию
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 4. Первый SUCCESS
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Transaction updatedTransaction =
                            transactionRepository.findById(transactionUid)
                                    .orElseThrow();

                    assertThat(updatedTransaction.getStatus())
                            .isEqualTo("SUCCESS");

                    Wallet updatedWallet =
                            walletRepository.findById(walletUid)
                                    .orElseThrow();

                    assertThat(updatedWallet.getBalance())
                            .isEqualByComparingTo("1000.00");
                });

        // 5. Тот же SUCCESS отправляем второй раз
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // Даём consumer обработать duplicate
        Thread.sleep(1000);

        // 6. Проверяем, что деньги второй раз НЕ зачислились
        Transaction finalTransaction =
                transactionRepository.findById(transactionUid)
                        .orElseThrow();

        assertThat(finalTransaction.getStatus())
                .isEqualTo("SUCCESS");

        Wallet finalWallet =
                walletRepository.findById(walletUid)
                        .orElseThrow();

        assertThat(finalWallet.getBalance())
                .isEqualByComparingTo("1000.00");
    }


    @Test
    void shouldNotCreditBalanceTwiceForDuplicateSuccessEvent() throws Exception {

        // 1. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_DUPLICATE_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 2. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Duplicate success wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        UUID walletUid = wallet.getUid();

        // 3. Создаём PENDING DEPOSIT
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 4. Отправляем SUCCESS первый раз
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Wallet updatedWallet =
                            walletRepository.findById(walletUid)
                                    .orElseThrow();

                    assertThat(updatedWallet.getBalance())
                            .isEqualByComparingTo("1000.00");
                });

        // 5. Отправляем тот же SUCCESS второй раз
        kafkaTemplate.send(
                "payment.status.updated",
                transactionUid.toString(),
                message
        ).get(10, TimeUnit.SECONDS);

        // 6. Проверяем, что второй раз деньги НЕ начислились
        await()
                .during(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    Transaction updatedTransaction =
                            transactionRepository.findById(transactionUid)
                                    .orElseThrow();

                    assertThat(updatedTransaction.getStatus())
                            .isEqualTo("SUCCESS");

                    Wallet updatedWallet =
                            walletRepository.findById(walletUid)
                                    .orElseThrow();

                    assertThat(updatedWallet.getBalance())
                            .isEqualByComparingTo("1000.00");
                });
    }


    @Test
    void shouldRejectPaymentStatusEventForNonDepositTransaction() {

        // 1. Создаём тип кошелька
        WalletType walletType = new WalletType();
        walletType.setName("TEST_NON_DEPOSIT_USD");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");

        walletType = walletTypeRepository.save(walletType);

        // 2. Создаём кошелёк
        Wallet wallet = new Wallet();
        wallet.setName("Non deposit wallet");
        wallet.setWalletType(walletType);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus("ACTIVE");
        wallet.setBalance(new BigDecimal("5000.00"));

        wallet = walletRepository.save(wallet);

        UUID walletUid = wallet.getUid();

        // 3. Создаём НЕ DEPOSIT, а WITHDRAWAL
        Transaction transaction = new Transaction();
        transaction.setUserUid(UUID.randomUUID());
        transaction.setWallet(wallet);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setType("WITHDRAWAL");
        transaction.setStatus("PENDING");

        transaction = transactionRepository.save(transaction);

        UUID transactionUid = transaction.getUid();

        // 4. Формируем SUCCESS-событие
        String message = """
                {
                  "transactionUid": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(transactionUid);

        // 5. Consumer обязан отклонить такое событие
        assertThatThrownBy(() ->
                paymentStatusUpdatedConsumer.handle(message)
        ).isInstanceOf(RuntimeException.class);

        // 6. Проверяем, что транзакция НЕ завершилась
        Transaction unchangedTransaction =
                transactionRepository.findById(transactionUid)
                        .orElseThrow();

        assertThat(unchangedTransaction.getStatus())
                .isEqualTo("PENDING");

        // 7. И баланс остался прежним
        Wallet unchangedWallet =
                walletRepository.findById(walletUid)
                        .orElseThrow();

        assertThat(unchangedWallet.getBalance())
                .isEqualByComparingTo("5000.00");
    }

    @Test
    void shouldSendFailedPaymentStatusEventToDltAfterRetries() throws Exception {

        Map<String, Object> consumerProperties =
                new HashMap<>();

        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        );

        consumerProperties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "payment-status-dlt-test-" + UUID.randomUUID()
        );

        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        try (
                KafkaConsumer<String, String> consumer =
                        new KafkaConsumer<>(consumerProperties)
        ) {

            consumer.subscribe(
                    List.of("payment.status.updated.DLT")
            );

            String badMessage =
                    """
                    {
                      "transactionUid": "this-is-not-a-uuid",
                      "status": "SUCCESS"
                    }
                    """;

            kafkaTemplate.send(
                    "payment.status.updated",
                    "bad-payment-status-event",
                    badMessage
            ).get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> dltRecord =
                    null;

            long deadline =
                    System.currentTimeMillis() + 15_000;

            while (
                    dltRecord == null
                            && System.currentTimeMillis() < deadline
            ) {

                ConsumerRecords<String, String> records =
                        consumer.poll(
                                Duration.ofMillis(500)
                        );

                for (
                        ConsumerRecord<String, String> record :
                        records
                ) {

                    dltRecord = record;
                    break;
                }
            }

            assertThat(dltRecord)
                    .isNotNull();

            assertThat(dltRecord.value())
                    .isEqualTo(badMessage);
        }
    }


}