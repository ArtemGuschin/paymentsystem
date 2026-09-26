package com.artem.transactionservice.kafka;

import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusUpdatedConsumer {

    private final TransactionRepository transactionRepository;
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
                    transactionRepository.findById(event.getTransactionUid())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Transaction not found: "
                                                    + event.getTransactionUid()
                                    )
                            );
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
                        "Unsupported payment status: " + event.getStatus()
                );
            }

            transaction.setStatus(event.getStatus());
            transaction.setModifiedAt(java.time.LocalDateTime.now());

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