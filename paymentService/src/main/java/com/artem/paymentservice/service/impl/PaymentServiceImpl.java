package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.exception.PaymentMethodNotFoundException;
import com.artem.paymentservice.exception.PaymentProviderUnavailableException;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.provider.PaymentGateway;
import com.artem.paymentservice.provider.factory.PaymentProviderFactory;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentRepository;
import com.artem.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentStateService paymentStateService;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {

        log.info(
                "Starting payment processing. internalTransactionUid={}, methodId={}, amount={}, currency={}",
                request.getInternalTransactionUid(),
                request.getMethodId(),
                request.getAmount(),
                request.getCurrency()
        );

        /*
         * 1. Idempotency key
         *
         * internalTransactionUid из Individuals является
         * уникальным идентификатором одной бизнес-операции.
         */
        String internalTransactionId =
                request.getInternalTransactionUid().toString();

        /*
         * 2. Сначала проверяем, не был ли этот платёж
         * уже создан ранее.
         *
         * Это защищает обычный повторный HTTP-запрос.
         */
        Payment existingPayment =
                paymentRepository
                        .findByInternalTransactionId(internalTransactionId)
                        .orElse(null);

        if (existingPayment != null) {

            log.info(
                    "Idempotent payment request. Returning existing payment. " +
                            "paymentId={}, internalTransactionId={}, status={}, providerTransactionId={}",
                    existingPayment.getId(),
                    existingPayment.getInternalTransactionId(),
                    existingPayment.getStatus(),
                    existingPayment.getExternalTransactionId()
            );

            return new PaymentResponse(
                    existingPayment.getExternalTransactionId(),
                    PaymentStatus.valueOf(existingPayment.getStatus())
            );
        }

        /*
         * 3. Находим способ оплаты
         */
        PaymentMethod paymentMethod =
                paymentMethodRepository
                        .findEligibleById(
                                request.getMethodId().intValue(),
                                request.getCurrency()
                        )
                        .orElseThrow(() ->
                                new PaymentMethodNotFoundException(
                                        request.getMethodId()
                                )
                        );

        log.info(
                "Payment method found. methodId={}, provider={}, providerMethodType={}",
                paymentMethod.getId(),
                paymentMethod.getProvider().getName(),
                paymentMethod.getProviderMethodType()
        );

        /*
         * 4. Создаём Payment со статусом PENDING.
         *
         * Важно:
         * createPendingPayment() работает в отдельной транзакции
         * REQUIRES_NEW.
         *
         * Поэтому PENDING будет зафиксирован в БД
         * ещё до вызова внешнего провайдера.
         *
         * На уровне БД internal_transaction_id должен иметь
         * UNIQUE NOT NULL constraint.
         *
         * Это защищает от race condition:
         *
         * request A -> INSERT
         * request B -> INSERT -> UNIQUE violation
         */
        Payment payment;

        try {

            payment =
                    paymentStateService.createPendingPayment(
                            paymentMethod,
                            internalTransactionId,
                            request.getAmount(),
                            request.getCurrency()
                    );

        } catch (DataIntegrityViolationException ex) {

            /*
             * Конкурентный запрос уже успел создать Payment
             * с тем же internalTransactionId.
             *
             * Поэтому считаем это идемпотентным повтором.
             */
            log.info(
                    "Concurrent idempotent request detected. " +
                            "Loading existing payment. internalTransactionId={}",
                    internalTransactionId
            );

            Payment concurrentPayment =
                    paymentRepository
                            .findByInternalTransactionId(internalTransactionId)
                            .orElseThrow(() -> ex);

            log.info(
                    "Returning existing payment after concurrent insert. " +
                            "paymentId={}, internalTransactionId={}, status={}, providerTransactionId={}",
                    concurrentPayment.getId(),
                    concurrentPayment.getInternalTransactionId(),
                    concurrentPayment.getStatus(),
                    concurrentPayment.getExternalTransactionId()
            );

            return new PaymentResponse(
                    concurrentPayment.getExternalTransactionId(),
                    PaymentStatus.valueOf(concurrentPayment.getStatus())
            );
        }

        log.info(
                "Payment created. paymentId={}, internalTransactionId={}, status={}",
                payment.getId(),
                payment.getInternalTransactionId(),
                payment.getStatus()
        );

        /*
         * 5. Получаем нужный PaymentGateway
         */
        PaymentGateway paymentGateway;

        try {

            paymentGateway =
                    paymentProviderFactory.getProvider(
                            paymentMethod.getProvider().getName()
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to resolve payment provider. paymentId={}, provider={}",
                    payment.getId(),
                    paymentMethod.getProvider().getName(),
                    ex
            );

            /*
             * Компенсация выполняется в отдельной транзакции.
             */
            paymentStateService.markFailed(payment);

            throw ex;
        }

        /*
         * 6. Вызываем внешний Payment Provider
         */
        PaymentResponse providerResponse;

        try {

            providerResponse =
                    paymentGateway.processPayment(
                            request,
                            paymentMethod.getProviderMethodType()
                    );

        } catch (Exception ex) {

            log.error(
                    "Payment provider call failed. paymentId={}, internalTransactionId={}",
                    payment.getId(),
                    payment.getInternalTransactionId(),
                    ex
            );

            /*
             * Компенсируем ранее созданный PENDING-платёж:
             *
             * PENDING -> FAILED
             */
            paymentStateService.markFailed(payment);

            throw new PaymentProviderUnavailableException(ex);
        }

        /*
         * 7. Проверяем ответ провайдера
         */
        if (providerResponse == null) {

            log.error(
                    "Payment provider returned null response. paymentId={}",
                    payment.getId()
            );

            paymentStateService.markFailed(payment);

            throw new IllegalStateException(
                    "Payment provider returned null response"
            );
        }

        if (providerResponse.getProviderTransactionId() == null
                || providerResponse.getProviderTransactionId().isBlank()) {

            log.error(
                    "Payment provider returned empty transaction id. paymentId={}",
                    payment.getId()
            );

            paymentStateService.markFailed(payment);

            throw new IllegalStateException(
                    "Payment provider returned empty transaction id"
            );
        }

        if (providerResponse.getStatus() == null) {

            log.error(
                    "Payment provider returned null status. paymentId={}",
                    payment.getId()
            );

            paymentStateService.markFailed(payment);

            throw new IllegalStateException(
                    "Payment provider returned null status"
            );
        }

        log.info(
                "Payment provider response received. paymentId={}, providerTransactionId={}, status={}",
                payment.getId(),
                providerResponse.getProviderTransactionId(),
                providerResponse.getStatus()
        );

        /*
         * 8. Обновляем состояние Payment
         */
        PaymentStatus providerStatus =
                providerResponse.getStatus();

        switch (providerStatus) {

            case PENDING -> {

                paymentStateService.updateStatus(
                        payment,
                        providerResponse.getProviderTransactionId(),
                        PaymentStatus.PENDING
                );

                log.info(
                        "Payment remains PENDING. paymentId={}, providerTransactionId={}",
                        payment.getId(),
                        providerResponse.getProviderTransactionId()
                );
            }

            case SUCCESS -> {

                paymentStateService.updateStatus(
                        payment,
                        providerResponse.getProviderTransactionId(),
                        PaymentStatus.SUCCESS
                );

                log.info(
                        "Payment completed successfully. paymentId={}, providerTransactionId={}",
                        payment.getId(),
                        providerResponse.getProviderTransactionId()
                );
            }

            case FAILED -> {

                paymentStateService.updateStatus(
                        payment,
                        providerResponse.getProviderTransactionId(),
                        PaymentStatus.FAILED
                );

                log.warn(
                        "Payment failed at provider. paymentId={}, providerTransactionId={}",
                        payment.getId(),
                        providerResponse.getProviderTransactionId()
                );
            }
        }

        /*
         * 9. Возвращаем ответ вызывающему сервису
         */
        log.info(
                "Payment processing finished. paymentId={}, providerTransactionId={}, status={}",
                payment.getId(),
                providerResponse.getProviderTransactionId(),
                providerResponse.getStatus()
        );

        return providerResponse;
    }
}