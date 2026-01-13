package com.artem.transactionservice.service.impl;

import com.artem.transaction.model.*;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.enums.PaymentType;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.TopUpService;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpConfirmRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopUpServiceImpl implements TopUpService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public TopUpInitResponse init(TopUpInitRequest request) {
        log.info("TopUp init request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();

        if (userUid == null || walletUid == null) {
            throw new IllegalArgumentException("userUid and walletUid must not be null");
        }
        Wallet wallet = walletService.getActiveWallet(walletUid, userUid);

        if (wallet == null) {
            throw new RuntimeException("Wallet not found or not active");
        }

        BigDecimal fee = request.getAmount()
                .multiply(BigDecimal.valueOf(0.00));
        BigDecimal total = request.getAmount().add(fee);

        TopUpInitResponse response = new TopUpInitResponse();
        response.setAvailable(true);
        response.setFee(fee);
        response.setTotalAmount(total);

        log.info("TopUp init successful for wallet: {}", wallet.getUid());
        return response;
    }

    @Transactional
    @Override
    public TopUpConfirmResponse confirm(TopUpConfirmRequest request) {
        log.info("TopUp confirm request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();


        if (userUid == null || walletUid == null) {
            throw new IllegalArgumentException("userUid and walletUid must not be null");
        }

        try {

            Wallet wallet = walletService.getActiveWallet(walletUid, userUid);

            if (wallet == null) {
                throw new RuntimeException("Wallet not found or not active");
            }

            Transaction tx = new Transaction();
            tx.setUserUid(userUid);
            tx.setWallet(wallet);
            tx.setAmount(request.getAmount());
            tx.setType(PaymentType.DEPOSIT.name());
            tx.setStatus("PENDING");

            transactionRepository.save(tx);
            log.info("Transaction created: {}", tx.getUid());

            kafkaTemplate.send(
                    "deposit.requested",
                    tx.getUid().toString(),
                    tx.getUid().toString()
            );

            TopUpConfirmResponse response = new TopUpConfirmResponse();
            response.setTransactionUid(tx.getUid());
            response.setStatus("PENDING");

            return response;

        } catch (Exception e) {
            log.error("Error in TopUp confirm: {}", e.getMessage(), e);
            throw e;
        }
    }
}
