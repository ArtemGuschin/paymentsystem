package com.artem.paymentservice.provider.fake;

import com.artem.fakepaymentprovider.client.api.TransactionsApi;
import com.artem.fakepaymentprovider.client.dto.Transaction;
import com.artem.fakepaymentprovider.client.dto.TransactionRequest;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.mapper.TransactionMapper;
import com.artem.paymentservice.provider.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("FAKE")
@RequiredArgsConstructor
public class FakePaymentGateway implements PaymentGateway {

    private static final int MAX_STATUS_ATTEMPTS = 5;
    private static final long STATUS_POLL_INTERVAL_MS = 500L;

    private final TransactionsApi transactionsApi;
    private final TransactionMapper transactionMapper;

    @Override
    public PaymentResponse processPayment(
            PaymentRequest request,
            String providerMethodType
    ) {

        log.info(
                "Creating transaction in Fake Payment Provider. internalTransactionUid={}, method={}",
                request.getInternalTransactionUid(),
                providerMethodType
        );

        /*
         * 1. Формируем запрос для Fake Payment Provider.
         */
        TransactionRequest transactionRequest =
                transactionMapper.toTransactionRequest(
                        request,
                        providerMethodType
                );

        /*
         * 2. Создаём транзакцию.
         */
        Transaction providerTransaction =
                transactionsApi.createTransaction(transactionRequest);

        if (providerTransaction == null
                || providerTransaction.getId() == null) {

            throw new IllegalStateException(
                    "Fake Payment Provider returned transaction without id"
            );
        }

        Long providerTransactionId =
                providerTransaction.getId();

        log.info(
                "Transaction created in Fake Payment Provider. providerTransactionId={}, initialStatus={}",
                providerTransactionId,
                providerTransaction.getStatus()
        );

        /*
         * 3. Получаем актуальный статус через REST.
         *
         * Webhook здесь специально НЕ используется.
         */
        Transaction actualTransaction =
                waitForFinalStatus(providerTransactionId);

        PaymentStatus paymentStatus =
                mapPaymentStatus(actualTransaction.getStatus().name());

        log.info(
                "Final provider transaction status received. providerTransactionId={}, status={}",
                providerTransactionId,
                actualTransaction.getStatus()
        );

        /*
         * 4. Возвращаем Payment Service результат
         * в его внутреннем формате.
         */
        return new PaymentResponse()
                .providerTransactionId(
                        providerTransactionId.toString()
                )
                .status(paymentStatus);
    }

    private Transaction waitForFinalStatus(
            Long providerTransactionId
    ) {

        Transaction transaction = null;

        for (int attempt = 1;
             attempt <= MAX_STATUS_ATTEMPTS;
             attempt++) {

            transaction =
                    transactionsApi.getTransactionById(
                            providerTransactionId
                    );

            if (transaction == null
                    || transaction.getStatus() == null) {

                throw new IllegalStateException(
                        "Fake Payment Provider returned invalid transaction status"
                );
            }

            String status =
                    transaction.getStatus().name();

            log.info(
                    "Polling provider transaction. providerTransactionId={}, attempt={}, status={}",
                    providerTransactionId,
                    attempt,
                    status
            );

            if ("SUCCESS".equals(status)
                    || "FAILED".equals(status)) {

                return transaction;
            }

            if (!"PENDING".equals(status)) {

                throw new IllegalStateException(
                        "Unknown transaction status from Fake Payment Provider: "
                                + status
                );
            }

            if (attempt < MAX_STATUS_ATTEMPTS) {
                sleepBeforeNextAttempt();
            }
        }

        throw new IllegalStateException(
                "Payment provider transaction did not reach final status in time. "
                        + "providerTransactionId=" + providerTransactionId
        );
    }

    private void sleepBeforeNextAttempt() {

        try {

            Thread.sleep(STATUS_POLL_INTERVAL_MS);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Payment status polling was interrupted",
                    ex
            );
        }
    }

    private PaymentStatus mapPaymentStatus(
            String providerStatus
    ) {

        return switch (providerStatus) {

            case "PENDING" ->
                    PaymentStatus.PENDING;

            case "SUCCESS" ->
                    PaymentStatus.SUCCESS;

            case "FAILED" ->
                    PaymentStatus.FAILED;

            default ->
                    throw new IllegalStateException(
                            "Unknown provider status: "
                                    + providerStatus
                    );
        };
    }
}