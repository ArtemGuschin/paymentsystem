package com.artem.transactionservice.service.impl;

import com.artem.transaction.model.WithdrawalConfirmRequest;
import com.artem.transaction.model.WithdrawalConfirmResponse;
import com.artem.transaction.model.WithdrawalInitRequest;
import com.artem.transaction.model.WithdrawalInitResponse;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.WalletService;
import com.artem.transactionservice.service.WithdrawalService;
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
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public WithdrawalInitResponse init(WithdrawalInitRequest request) {
        log.info("Withdrawal INIT method entered");
        log.info("Withdrawal init request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();
        BigDecimal amount = request.getAmount();


        if (userUid == null || walletUid == null || amount == null) {
            throw new InvalidRequestException("userUid, walletUid and amount are required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }


        Wallet wallet = walletService.getActiveWallet(walletUid, userUid);


        BigDecimal fee = amount.multiply(BigDecimal.valueOf(0.00));


        BigDecimal totalAmount = amount.add(fee);


        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            WithdrawalInitResponse response = new WithdrawalInitResponse();
            response.setAvailable(false);
            response.setMessage("Insufficient funds");
            response.setFee(fee);
            response.setTotalAmount(totalAmount);
            response.setCurrency(wallet.getWalletType().getCurrencyCode());
            return response;
        }


        WithdrawalInitResponse response = new WithdrawalInitResponse();
        response.setAvailable(true);
        response.setFee(fee);
        response.setTotalAmount(totalAmount);
        response.setCurrency(wallet.getWalletType().getCurrencyCode());
        response.setMessage("Withdrawal is available");

        log.info("Withdrawal init successful for wallet: {}", wallet.getUid());

        return response;
    }

    @Override
    public WithdrawalConfirmResponse confirm(WithdrawalConfirmRequest request) {

        log.info("Withdrawal confirm request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount());

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();
        BigDecimal amount = request.getAmount();


        if (userUid == null || walletUid == null || amount == null) {
            throw new InvalidRequestException("userUid, walletUid and amount are required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }


        Wallet wallet = walletService.getActiveWallet(walletUid, userUid);


        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }


        Transaction tx = new Transaction();
        tx.setUserUid(userUid);
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType("WITHDRAWAL");
        tx.setStatus("PENDING");
        tx.setComment(request.getComment());

        transactionRepository.save(tx);

        log.info("Withdrawal transaction created: {}", tx.getUid());


        kafkaTemplate.send(
                "withdrawal.requested",
                tx.getUid().toString(),
                tx.getUid().toString()
        );


        WithdrawalConfirmResponse response = new WithdrawalConfirmResponse();
        response.setTransactionUid(tx.getUid());
        response.setStatus("PENDING");

        return response;
    }
}
