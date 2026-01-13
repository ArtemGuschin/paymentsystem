package com.artem.transactionservice.kafka;

import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositConsumer {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Transactional
    @KafkaListener(
            topics = "deposit.requested",
            groupId = "transaction-service"
    )
    public void handleDeposit(String transactionUid) {

        log.info("Deposit event received, tx={}", transactionUid);

        Transaction tx = transactionRepository.findById(
                UUID.fromString(transactionUid)
        ).orElseThrow(() -> new RuntimeException("Transaction not found"));

        walletRepository.increaseBalance(
                tx.getWallet().getUid(),
                tx.getAmount()
        );

        tx.setStatus("COMPLETED");
        transactionRepository.save(tx);
    }
}
