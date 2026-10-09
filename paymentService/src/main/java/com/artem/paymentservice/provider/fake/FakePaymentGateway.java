package com.artem.paymentservice.provider.fake;

import com.artem.fakepaymentprovider.client.api.TransactionsApi;
import com.artem.fakepaymentprovider.client.dto.Transaction;
import com.artem.fakepaymentprovider.client.dto.TransactionRequest;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.exception.PaymentResultUnknownException;
import com.artem.paymentservice.mapper.TransactionMapper;
import com.artem.paymentservice.provider.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Consumer;

@Slf4j
@Component("FAKE")
@RequiredArgsConstructor
public class FakePaymentGateway implements PaymentGateway {

    private static final int MAX_STATUS_ATTEMPTS = 5;
    private static final long STATUS_POLL_INTERVAL_MS = 500L;

    private final TransactionsApi transactionsApi;
    private final TransactionMapper transactionMapper;

    @Value("${webhook.collector-url}")
    private String webhookCollectorUrl;

    @Override
    public PaymentResponse processPayment(
            PaymentRequest request,
            String providerMethodType,
            Consumer<String> providerTransactionIdConsumer
    ) {

        log.info(
                "Creating transaction in Fake Payment Provider. internalTransactionUid={}, method={}",
                request.getInternalTransactionUid(),
                providerMethodType
        );

        TransactionRequest transactionRequest =
                transactionMapper.toTransactionRequest(
                        request,
                        providerMethodType
                );

        transactionRequest.setNotificationUrl(
                webhookCollectorUrl
        );

        log.info(
                "ApiClient basePath={}",
                transactionsApi.getApiClient().getBasePath()
        );

        Transaction providerTransaction;

        try {

            providerTransaction =
                    transactionsApi.createTransaction(
                            transactionRequest
                    );

        } catch (HttpClientErrorException ex) {

            /*
             * 409 означает:
             *
             * provider уже имеет транзакцию с таким externalId.
             *
             * Новую транзакцию не создаём.
             * Восстанавливаем существующую.
             */
            if (ex.getStatusCode().value() == 409) {

                log.info(
                        "Provider transaction already exists. " +
                                "Recovering by externalId={}",
                        request.getInternalTransactionUid()
                );

                providerTransaction =
                        recoverTransactionByExternalId(
                                request,
                                ex
                        );

            } else {

                log.error(
                        "Provider returned HTTP error while creating transaction. " +
                                "status={}, internalTransactionUid={}",
                        ex.getStatusCode(),
                        request.getInternalTransactionUid(),
                        ex
                );

                throw ex;
            }

        } catch (ResourceAccessException createEx) {

            /*
             * Транспортная ошибка.
             *
             * Provider мог получить POST и создать транзакцию,
             * но HTTP-ответ мог потеряться.
             *
             * Поэтому нельзя считать операцию FAILED.
             * Пытаемся найти её по externalId.
             */
            log.warn(
                    "Create transaction response was lost. " +
                            "Trying to recover transaction by externalId={}",
                    request.getInternalTransactionUid(),
                    createEx
            );

            providerTransaction =
                    recoverTransactionByExternalId(
                            request,
                            createEx
                    );

        } catch (Exception ex) {

            /*
             * Обычная ошибка вызова provider.
             */
            log.error(
                    "Provider call failed. internalTransactionUid={}",
                    request.getInternalTransactionUid(),
                    ex
            );

            throw ex;
        }

        if (providerTransaction == null
                || providerTransaction.getId() == null) {

            throw new PaymentResultUnknownException(
                    "Fake Payment Provider transaction exists, " +
                            "but provider transaction id is unavailable",
                    null
            );
        }

        Long providerTransactionId =
                providerTransaction.getId();

        log.info(
                "Transaction accepted by Fake Payment Provider. " +
                        "providerTransactionId={}, initialStatus={}",
                providerTransactionId,
                providerTransaction.getStatus()
        );

        /*
         * КРИТИЧЕСКИЙ ПОРЯДОК:
         *
         * 1. Provider уже создал транзакцию.
         * 2. Получен providerTransactionId.
         * 3. Сначала сохраняем ID в нашей БД.
         * 4. Только затем начинаем polling.
         */
        try {

            providerTransactionIdConsumer.accept(
                    providerTransactionId.toString()
            );

        } catch (Exception saveEx) {

            /*
             * Provider уже создал операцию,
             * но мы не смогли зафиксировать его ID локально.
             *
             * Это НЕ FAILED.
             */
            throw new PaymentResultUnknownException(
                    "Provider transaction was created, " +
                            "but providerTransactionId could not be persisted",
                    saveEx
            );
        }

        /*
         * providerTransactionId уже сохранён.
         *
         * Теперь безопасно узнавать финальный статус.
         */
        try {

            Transaction actualTransaction =
                    waitForFinalStatus(
                            providerTransactionId
                    );

            PaymentStatus paymentStatus =
                    mapPaymentStatus(
                            actualTransaction
                                    .getStatus()
                                    .name()
                    );

            log.info(
                    "Final provider transaction status received. " +
                            "providerTransactionId={}, status={}",
                    providerTransactionId,
                    actualTransaction.getStatus()
            );

            return new PaymentResponse()
                    .providerTransactionId(
                            providerTransactionId.toString()
                    )
                    .status(
                            paymentStatus
                    );

        } catch (Exception ex) {

            /*
             * ID уже сохранён.
             *
             * Ошибка polling не означает FAILED.
             * Reconciliation сможет продолжить позже.
             */
            log.warn(
                    "Could not determine final provider status. " +
                            "Keeping payment PENDING. providerTransactionId={}",
                    providerTransactionId,
                    ex
            );

            return new PaymentResponse()
                    .providerTransactionId(
                            providerTransactionId.toString()
                    )
                    .status(
                            PaymentStatus.PENDING
                    );
        }
    }

    /**
     * Ищем уже существующую provider-транзакцию
     * по нашему internalTransactionUid,
     * который отправляется provider как externalId.
     */
    private Transaction recoverTransactionByExternalId(
            PaymentRequest request,
            Exception originalException
    ) {

        String externalId =
                request.getInternalTransactionUid().toString();

        try {

            Transaction recoveredTransaction =
                    transactionsApi.getTransactionByExternalId(
                            externalId
                    );

            if (recoveredTransaction == null
                    || recoveredTransaction.getId() == null) {

                throw new IllegalStateException(
                        "Recovered provider transaction has no id"
                );
            }

            log.info(
                    "Provider transaction recovered by externalId. " +
                            "externalId={}, providerTransactionId={}, status={}",
                    externalId,
                    recoveredTransaction.getId(),
                    recoveredTransaction.getStatus()
            );

            return recoveredTransaction;

        } catch (Exception recoveryEx) {

            /*
             * Provider мог создать транзакцию,
             * но сейчас мы не можем доказать её состояние.
             *
             * Поэтому UNKNOWN, а не FAILED.
             */
            log.warn(
                    "Could not recover provider transaction by externalId={}. " +
                            "Payment result is unknown.",
                    externalId,
                    recoveryEx
            );

            throw new PaymentResultUnknownException(
                    "Payment provider may have created the transaction, " +
                            "but its result could not be recovered",
                    originalException
            );
        }
    }

    @Override
    public PaymentStatus getPaymentStatus(
            String providerTransactionId
    ) {

        Long providerId;

        try {

            providerId =
                    Long.parseLong(
                            providerTransactionId
                    );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Invalid provider transaction id: "
                            + providerTransactionId,
                    ex
            );
        }

        Transaction transaction =
                transactionsApi.getTransactionById(
                        providerId
                );

        if (transaction == null
                || transaction.getStatus() == null) {

            throw new IllegalStateException(
                    "Fake Payment Provider returned invalid transaction status. "
                            + "providerTransactionId="
                            + providerTransactionId
            );
        }

        return mapPaymentStatus(
                transaction.getStatus().name()
        );
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
                    transaction
                            .getStatus()
                            .name();

            log.info(
                    "Polling provider transaction. " +
                            "providerTransactionId={}, attempt={}, status={}",
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
                        + "providerTransactionId="
                        + providerTransactionId
        );
    }

    private void sleepBeforeNextAttempt() {

        try {

            Thread.sleep(
                    STATUS_POLL_INTERVAL_MS
            );

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