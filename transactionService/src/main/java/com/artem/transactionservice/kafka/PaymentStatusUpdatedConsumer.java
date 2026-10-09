package com.artem.transactionservice.kafka;

import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.enums.PaymentType;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusUpdatedConsumer {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            id = "paymentStatusUpdatedListener",
            topics = "payment.status.updated",
            groupId = "transaction-service",
            autoStartup = "${payment-status-consumer.auto-startup:true}"
    )
    @Transactional
    public void handle(String message) {

        log.info("Payment status event received: {}", message);

        try {

            PaymentStatusUpdatedEvent event =
                    objectMapper.readValue(
                            message,
                            PaymentStatusUpdatedEvent.class
                    );

            Transaction transaction =
                    transactionRepository
                            .findByIdForUpdate(
                                    event.getTransactionUid()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Transaction not found: "
                                                    + event.getTransactionUid()
                                    )
                            );

            /*
             * payment.status.updated сейчас используется
             * для финализации пополнения.
             *
             * Чужой тип финансовой операции
             * нельзя случайно зачислить на кошелёк.
             */
            if (!PaymentType.DEPOSIT.name()
                    .equals(transaction.getType())) {

                throw new IllegalStateException(
                        "Payment status event received for non-DEPOSIT transaction: "
                                + transaction.getUid()
                );
            }

            /*
             * Идемпотентность.
             *
             * SUCCESS уже обработан -> второй раз деньги
             * не зачисляем.
             *
             * FAILED уже обработан -> тоже ничего не делаем.
             */
            if (!"PENDING".equals(transaction.getStatus())) {

                log.warn(
                        "Payment status event already processed. transactionUid={}, currentStatus={}, receivedStatus={}",
                        transaction.getUid(),
                        transaction.getStatus(),
                        event.getStatus()
                );

                return;
            }

            if (!"SUCCESS".equals(event.getStatus())
                    && !"FAILED".equals(event.getStatus())) {

                throw new IllegalArgumentException(
                        "Unsupported payment status: "
                                + event.getStatus()
                );
            }

            if ("SUCCESS".equals(event.getStatus())) {

                int updatedWallets =
                        walletRepository.increaseBalance(
                                transaction.getWallet().getUid(),
                                transaction.getAmount()
                        );

                if (updatedWallets != 1) {
                    throw new IllegalStateException(
                            "Failed to increase wallet balance. walletUid="
                                    + transaction.getWallet().getUid()
                    );
                }

                transaction.setStatus("SUCCESS");

                log.info(
                        "Deposit completed. transactionUid={}, walletUid={}, amount={}",
                        transaction.getUid(),
                        transaction.getWallet().getUid(),
                        transaction.getAmount()
                );

            } else {

                /*
                 * FAILED:
                 * баланс вообще не трогаем.
                 */
                transaction.setStatus("FAILED");

                log.info(
                        "Deposit failed. transactionUid={}",
                        transaction.getUid()
                );
            }

            transaction.setModifiedAt(LocalDateTime.now());

            transactionRepository.save(transaction);

            log.info(
                    "Transaction finalized. transactionUid={}, status={}",
                    transaction.getUid(),
                    transaction.getStatus()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process payment.status.updated. message={}",
                    message,
                    e
            );

            throw new RuntimeException(e);
        }
    }
}