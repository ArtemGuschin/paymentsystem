package com.artem.transactionservice.service.impl;

import com.artem.transaction.model.*;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.TopUpService;
import com.artem.transactionservice.service.TransferService;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public TransferInitResponse init(TransferInitRequest request) {

        log.info("Transfer init request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID sourceWalletUid = request.getWalletUid();
        UUID targetWalletUid = request.getTargetWalletUid();
        BigDecimal amount = request.getAmount();

        // 1️⃣ Валидация
        if (userUid == null || sourceWalletUid == null || targetWalletUid == null || amount == null) {
            throw new InvalidRequestException("userUid, walletUid, targetWalletUid, amount");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }

        if (sourceWalletUid.equals(targetWalletUid)) {
            throw new InvalidRequestException("source and target wallets must be different");
        }

        // 2️⃣ Проверяем source wallet
        Wallet sourceWallet = walletService.getActiveWallet(sourceWalletUid, userUid);

        boolean available = sourceWallet.getBalance().compareTo(amount) >= 0;

        // 3️⃣ Ответ
        TransferInitResponse response = new TransferInitResponse();
        response.setAvailable(available);
        response.setTotalAmount(amount);
        response.setFee(BigDecimal.ZERO);      // пока 0
        response.setCurrency("RUB");           // или из wallet
        response.setMessage(
                available
                        ? "Transfer is available"
                        : "Insufficient funds"
        );

        return response;
    }


    @Override
    public TransferConfirmResponse confirm(TransferConfirmRequest request) {

        log.info("Transfer confirm request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID sourceWalletUid = request.getWalletUid();
        UUID targetWalletUid = request.getTargetWalletUid();
        BigDecimal amount = request.getAmount();

        // 1️⃣ Валидация
        if (userUid == null || sourceWalletUid == null || targetWalletUid == null || amount == null) {
            throw new InvalidRequestException("userUid, walletUid, targetWalletUid, amount are required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }

        if (sourceWalletUid.equals(targetWalletUid)) {
            throw new InvalidRequestException("source and target wallets must be different");
        }

        // 2️⃣ Проверяем source wallet
        Wallet sourceWallet = walletService.getActiveWallet(sourceWalletUid, userUid);

        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // 3️⃣ Создаём транзакцию
        Transaction tx = new Transaction();
        tx.setUserUid(userUid);
        tx.setWallet(sourceWallet);
        tx.setTargetWalletUid(targetWalletUid);
        tx.setAmount(amount);
        tx.setType("TRANSFER");
        tx.setStatus("PENDING");
        tx.setComment(request.getComment());

        transactionRepository.save(tx);

        log.info("Transfer transaction created: {}", tx.getUid());

        // 4️⃣ Kafka
        kafkaTemplate.send(
                "transfer.requested",
                tx.getUid().toString(), // key
                tx.getUid().toString()  // value
        );

        // 5️⃣ Ответ
        TransferConfirmResponse response = new TransferConfirmResponse();
        response.setTransactionUid(tx.getUid());
        response.setStatus("PENDING");

        return response;
    }

}
