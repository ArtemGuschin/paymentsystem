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
public class WithdrawalConsumer {
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    @KafkaListener(
            topics = "withdrawal.requested",
            groupId = "transaction-service"
    )

    @Transactional
    public void handleWithdrawal(String transactionUid) {

        log.info("Received withdrawal event, tx={}", transactionUid);

        Transaction tx = transactionRepository.findById(UUID.fromString(transactionUid))
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 🔐 защита от двойной обработки
        if (!"PENDING".equals(tx.getStatus())) {
            log.warn("Transaction already processed, tx={}, status={}",
                    tx.getUid(), tx.getStatus());
            return;
        }

        try {
            walletService.decreaseBalance(
                    tx.getWallet().getUid(),
                    tx.getUserUid(),
                    tx.getAmount()
            );

            tx.setStatus("COMPLETED");
            transactionRepository.save(tx);

            log.info("Withdrawal completed, tx={}", transactionUid);

        } catch (Exception e) {
            tx.setStatus("FAILED");
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);

            log.error("Withdrawal failed, tx={}", transactionUid, e);
        }
    }


}
