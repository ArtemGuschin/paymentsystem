package com.artem.transactionservice.service.impl;

import com.artem.transaction.model.TopUpCompleteRequest;
import com.artem.transaction.model.TopUpConfirmRequest;
import com.artem.transaction.model.TopUpConfirmResponse;
import com.artem.transaction.model.TopUpFailRequest;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpInitResponse;
import com.artem.transaction.model.TopUpResultResponse;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.enums.PaymentType;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.TopUpService;
import com.artem.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopUpServiceImpl implements TopUpService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;

    @Override
    public TopUpInitResponse init(TopUpInitRequest request) {

        log.info(
                "TopUp init request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount()
        );

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();

        if (userUid == null || walletUid == null) {
            throw new IllegalArgumentException(
                    "userUid and walletUid must not be null"
            );
        }

        Wallet wallet =
                walletService.getActiveWallet(
                        walletUid,
                        userUid
                );

        if (wallet == null) {
            throw new RuntimeException(
                    "Wallet not found or not active"
            );
        }

        BigDecimal fee =
                request.getAmount()
                        .multiply(BigDecimal.ZERO);

        BigDecimal total =
                request.getAmount()
                        .add(fee);

        TopUpInitResponse response =
                new TopUpInitResponse();

        response.setAvailable(true);
        response.setFee(fee);
        response.setTotalAmount(total);

        log.info(
                "TopUp init successful for wallet: {}",
                wallet.getUid()
        );

        return response;
    }

    /**
     * Создаём только PENDING.
     *
     * ВАЖНО:
     * здесь больше нет Kafka и deposit.requested.
     */
    @Transactional
    @Override
    public TopUpConfirmResponse confirm(
            TopUpConfirmRequest request
    ) {

        log.info(
                "TopUp confirm request: userUid={}, walletUid={}, amount={}",
                request.getUserUid(),
                request.getWalletUid(),
                request.getAmount()
        );

        UUID userUid = request.getUserUid();
        UUID walletUid = request.getWalletUid();

        if (userUid == null || walletUid == null) {
            throw new IllegalArgumentException(
                    "userUid and walletUid must not be null"
            );
        }

        Wallet wallet =
                walletService.getActiveWallet(
                        walletUid,
                        userUid
                );

        if (wallet == null) {
            throw new RuntimeException(
                    "Wallet not found or not active"
            );
        }

        Transaction transaction =
                new Transaction();

        transaction.setUserUid(userUid);
        transaction.setWallet(wallet);
        transaction.setAmount(request.getAmount());
        transaction.setType(PaymentType.DEPOSIT.name());
        transaction.setStatus("PENDING");
        transaction.setComment(request.getComment());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        log.info(
                "TopUp transaction created. transactionUid={}, status=PENDING",
                savedTransaction.getUid()
        );

        TopUpConfirmResponse response =
                new TopUpConfirmResponse();

        response.setTransactionUid(
                savedTransaction.getUid()
        );

        response.setStatus(
                TopUpConfirmResponse.StatusEnum.PENDING
        );

        return response;
    }

    /**
     * Успешное завершение платежа.
     *
     * Идемпотентность:
     * если transaction уже COMPLETED,
     * повторное зачисление не выполняется.
     */
    @Transactional
    @Override
    public TopUpResultResponse completeTopUp(
            UUID transactionUid,
            TopUpCompleteRequest request
    ) {

        log.info(
                "Complete top up request. transactionUid={}, providerTransactionId={}",
                transactionUid,
                request.getProviderTransactionId()
        );

        if (transactionUid == null) {
            throw new IllegalArgumentException(
                    "transactionUid must not be null"
            );
        }

        if (request == null ||
                request.getProviderTransactionId() == null ||
                request.getProviderTransactionId().isBlank()) {

            throw new IllegalArgumentException(
                    "providerTransactionId must not be null or blank"
            );
        }

        Transaction transaction =
                transactionRepository
                        .findByUidForUpdate(transactionUid)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found: "
                                                + transactionUid
                                )
                        );

        /*
         * Идемпотентный повтор:
         * деньги уже были зачислены.
         */
        if ("COMPLETED".equals(transaction.getStatus())) {

            log.info(
                    "TopUp already completed. transactionUid={}, providerTransactionId={}",
                    transaction.getUid(),
                    transaction.getProviderTransactionId()
            );

            return toResultResponse(transaction);
        }

        /*
         * FAILED нельзя перевести обратно в SUCCESS.
         */
        if ("FAILED".equals(transaction.getStatus())) {

            throw new IllegalStateException(
                    "Cannot complete FAILED transaction: "
                            + transactionUid
            );
        }

        /*
         * Только PENDING можно завершить успешно.
         */
        if (!"PENDING".equals(transaction.getStatus())) {

            throw new IllegalStateException(
                    "Cannot complete transaction with status: "
                            + transaction.getStatus()
            );
        }

        /*
         * Сохраняем внешний ID до завершения операции.
         */
        transaction.setProviderTransactionId(
                request.getProviderTransactionId()
        );

        /*
         * Идемпотентное зачисление.
         *
         * Transaction заблокирована через PESSIMISTIC_WRITE,
         * поэтому параллельный запрос дождётся lock
         * и увидит уже COMPLETED.
         */
        walletService.increaseBalance(
                transaction.getWallet().getUid(),
                transaction.getUserUid(),
                transaction.getAmount()
        );

        transaction.setStatus("COMPLETED");
        transaction.setModifiedAt(
                java.time.LocalDateTime.now()
        );

        Transaction saved =
                transactionRepository.save(transaction);

        log.info(
                "TopUp completed successfully. transactionUid={}, providerTransactionId={}, amount={}",
                saved.getUid(),
                saved.getProviderTransactionId(),
                saved.getAmount()
        );

        return toResultResponse(saved);
    }

    /**
     * Неуспешное завершение платежа.
     */
    @Transactional
    @Override
    public TopUpResultResponse failTopUp(
            UUID transactionUid,
            TopUpFailRequest request
    ) {

        log.warn(
                "Fail top up request. transactionUid={}",
                transactionUid
        );

        if (transactionUid == null) {
            throw new IllegalArgumentException(
                    "transactionUid must not be null"
            );
        }

        Transaction transaction =
                transactionRepository
                        .findByUidForUpdate(transactionUid)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found: "
                                                + transactionUid
                                )
                        );

        /*
         * Идемпотентный повтор FAILED.
         */
        if ("FAILED".equals(transaction.getStatus())) {

            log.info(
                    "TopUp already failed. transactionUid={}",
                    transaction.getUid()
            );

            return toResultResponse(transaction);
        }

        /*
         * Уже успешный платёж нельзя "откатить"
         * простым вызовом fail endpoint.
         */
        if ("COMPLETED".equals(transaction.getStatus())) {

            throw new IllegalStateException(
                    "Cannot fail COMPLETED transaction: "
                            + transactionUid
            );
        }

        if (!"PENDING".equals(transaction.getStatus())) {

            throw new IllegalStateException(
                    "Cannot fail transaction with status: "
                            + transaction.getStatus()
            );
        }

        if (request != null) {
            transaction.setFailureReason(
                    request.getFailureReason()
            );
        }

        transaction.setStatus("FAILED");
        transaction.setModifiedAt(
                java.time.LocalDateTime.now()
        );

        Transaction saved =
                transactionRepository.save(transaction);

        log.warn(
                "TopUp marked as FAILED. transactionUid={}, reason={}",
                saved.getUid(),
                saved.getFailureReason()
        );

        return toResultResponse(saved);
    }

    private TopUpResultResponse toResultResponse(
            Transaction transaction
    ) {

        TopUpResultResponse response =
                new TopUpResultResponse();

        response.setTransactionUid(
                transaction.getUid()
        );

        response.setStatus(
                mapStatus(transaction.getStatus())
        );

        response.setProviderTransactionId(
                transaction.getProviderTransactionId()
        );

        return response;
    }

    private TopUpResultResponse.StatusEnum mapStatus(
            String status
    ) {

        return switch (status) {
            case "PENDING" ->
                    TopUpResultResponse.StatusEnum.PENDING;

            case "COMPLETED" ->
                    TopUpResultResponse.StatusEnum.COMPLETED;

            case "FAILED" ->
                    TopUpResultResponse.StatusEnum.FAILED;

            default ->
                    throw new IllegalStateException(
                            "Unknown transaction status: "
                                    + status
                    );
        };
    }
}