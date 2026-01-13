package com.artem.transactionservice.kafka;

import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferConsumer {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    @KafkaListener(
            topics = "transfer.requested",
            groupId = "transaction-service"
    )
    @Transactional
    public void handleTransfer(String transactionUid) {

        log.info("Received transfer event, tx={}", transactionUid);

        Transaction tx = transactionRepository.findById(UUID.fromString(transactionUid))
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 🔐 идемпотентность
        if (!"PENDING".equals(tx.getStatus())) {
            log.warn("Transfer already processed, tx={}, status={}",
                    tx.getUid(), tx.getStatus());
            return;
        }

        try {
            // 1️⃣ списываем
            walletService.decreaseBalance(
                    tx.getWallet().getUid(),
                    tx.getUserUid(),
                    tx.getAmount()
            );

            // 2️⃣ зачисляем
            walletService.increaseBalance(
                    tx.getTargetWalletUid(),
                    tx.getUserUid(),
                    tx.getAmount()
            );

            // 3️⃣ фиксируем успех
            tx.setStatus("COMPLETED");
            transactionRepository.save(tx);

            log.info("Transfer completed, tx={}", tx.getUid());

        } catch (Exception e) {
            tx.setStatus("FAILED");
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);

            log.error("Transfer failed, tx={}", tx.getUid(), e);
        }
    }
}