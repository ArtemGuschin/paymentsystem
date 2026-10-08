package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.exception.PaymentIdempotencyConflictException;
import com.artem.paymentservice.exception.PaymentMethodNotFoundException;
import com.artem.paymentservice.exception.PaymentProviderUnavailableException;
import com.artem.paymentservice.exception.PaymentResultUnknownException;
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
         */
        String internalTransactionId =
                request.getInternalTransactionUid().toString();

        /*
         * 2. Проверяем существующий Payment.
         */
        Payment existingPayment =
                paymentRepository
                        .findByInternalTransactionId(
                                internalTransactionId
                        )
                        .orElse(null);

        /*
         * Один и тот же idempotency key разрешён
         * только для ТОГО ЖЕ бизнес-запроса.
         *
         * Если amount / currency / methodId изменились,
         * это уже конфликт, а не idempotent retry.
         */
        if (existingPayment != null) {

            validateIdempotentRequest(
                    existingPayment,
                    request
            );
        }

        /*
         * Особый recovery-сценарий:
         *
         * Payment уже существует локально,
         * имеет PENDING,
         * но providerTransactionId неизвестен.
         *
         * Это может означать, что provider получил create,
         * создал транзакцию, но HTTP-ответ потерялся.
         */
        boolean recoverExistingPayment =
                existingPayment != null
                        && PaymentStatus.PENDING.name()
                        .equals(existingPayment.getStatus())
                        && (
                        existingPayment.getExternalTransactionId() == null
                                || existingPayment
                                .getExternalTransactionId()
                                .isBlank()
                );

        /*
         * Обычный идемпотентный повтор.
         *
         * Параметры мы уже проверили выше.
         *
         * Если это НЕ специальный recovery-сценарий,
         * просто возвращаем уже сохранённый результат.
         */
        if (existingPayment != null
                && !recoverExistingPayment) {

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
                    PaymentStatus.valueOf(
                            existingPayment.getStatus()
                    )
            );
        }

        if (recoverExistingPayment) {

            log.warn(
                    "Found PENDING payment without providerTransactionId. " +
                            "Retrying provider recovery. paymentId={}, internalTransactionId={}",
                    existingPayment.getId(),
                    existingPayment.getInternalTransactionId()
            );
        }

        /*
         * 3. Находим способ оплаты.
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
         * 4. Либо используем существующий PENDING Payment
         * для recovery, либо создаём новый.
         */
        Payment payment;

        if (recoverExistingPayment) {

            payment = existingPayment;

            log.info(
                    "Reusing existing PENDING payment for provider recovery. " +
                            "paymentId={}, internalTransactionId={}",
                    payment.getId(),
                    payment.getInternalTransactionId()
            );

        } else {

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
                 * Конкурентный запрос уже успел создать Payment.
                 *
                 * Загружаем его и ОБЯЗАТЕЛЬНО сравниваем
                 * исходные параметры запроса.
                 */
                log.info(
                        "Concurrent idempotent request detected. " +
                                "Loading existing payment. internalTransactionId={}",
                        internalTransactionId
                );

                Payment concurrentPayment =
                        paymentRepository
                                .findByInternalTransactionId(
                                        internalTransactionId
                                )
                                .orElseThrow(() -> ex);

                /*
                 * Даже при race condition одинаковый UUID
                 * с другими параметрами должен дать 409.
                 */
                validateIdempotentRequest(
                        concurrentPayment,
                        request
                );

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
                        PaymentStatus.valueOf(
                                concurrentPayment.getStatus()
                        )
                );
            }
        }

        log.info(
                "Payment ready for provider processing. " +
                        "paymentId={}, internalTransactionId={}, status={}, providerTransactionId={}",
                payment.getId(),
                payment.getInternalTransactionId(),
                payment.getStatus(),
                payment.getExternalTransactionId()
        );

        /*
         * 5. Получаем нужный PaymentGateway.
         */
        PaymentGateway paymentGateway;

        try {

            paymentGateway =
                    paymentProviderFactory.getProvider(
                            paymentMethod
                                    .getProvider()
                                    .getName()
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to resolve payment provider. paymentId={}, provider={}",
                    payment.getId(),
                    paymentMethod.getProvider().getName(),
                    ex
            );

            /*
             * Для нового платежа это локальная ошибка до обращения
             * к provider, поэтому можно завершить FAILED.
             *
             * Для recovery ранее provider уже мог принять операцию,
             * поэтому её состояние оставляем PENDING.
             */
            if (!recoverExistingPayment) {

                paymentStateService.markFailed(
                        payment
                );
            }

            throw ex;
        }

        /*
         * 6. Вызываем внешний Payment Provider.
         */
        PaymentResponse providerResponse;

        try {

            providerResponse =
                    paymentGateway.processPayment(
                            request,
                            paymentMethod.getProviderMethodType(),
                            providerTransactionId ->
                                    paymentStateService
                                            .saveProviderTransactionId(
                                                    payment,
                                                    providerTransactionId
                                            )
                    );

        } catch (PaymentResultUnknownException ex) {

            /*
             * Provider мог уже создать операцию,
             * но её результат сейчас неизвестен.
             *
             * FAILED ставить нельзя.
             */
            log.warn(
                    "Payment result is unknown. Keeping payment PENDING. " +
                            "paymentId={}, internalTransactionId={}",
                    payment.getId(),
                    payment.getInternalTransactionId(),
                    ex
            );

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Payment provider call failed. paymentId={}, internalTransactionId={}",
                    payment.getId(),
                    payment.getInternalTransactionId(),
                    ex
            );

            /*
             * Обычная ошибка provider.
             *
             * Если это recovery уже неизвестной операции,
             * не переводим её в FAILED.
             */
            if (!recoverExistingPayment) {

                paymentStateService.markFailed(
                        payment
                );
            }

            throw new PaymentProviderUnavailableException(
                    ex
            );
        }

        /*
         * 7. Проверяем ответ провайдера.
         */
        if (providerResponse == null) {

            log.error(
                    "Payment provider returned null response. paymentId={}",
                    payment.getId()
            );

            if (!recoverExistingPayment) {

                paymentStateService.markFailed(
                        payment
                );
            }

            throw new IllegalStateException(
                    "Payment provider returned null response"
            );
        }

        if (providerResponse.getProviderTransactionId() == null
                || providerResponse
                .getProviderTransactionId()
                .isBlank()) {

            log.error(
                    "Payment provider returned empty transaction id. paymentId={}",
                    payment.getId()
            );

            if (!recoverExistingPayment) {

                paymentStateService.markFailed(
                        payment
                );
            }

            throw new IllegalStateException(
                    "Payment provider returned empty transaction id"
            );
        }

        if (providerResponse.getStatus() == null) {

            log.error(
                    "Payment provider returned null status. paymentId={}",
                    payment.getId()
            );

            if (!recoverExistingPayment) {

                paymentStateService.markFailed(
                        payment
                );
            }

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
         * 8. Обновляем состояние Payment.
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
         * 9. Возвращаем ответ.
         */
        log.info(
                "Payment processing finished. paymentId={}, providerTransactionId={}, status={}",
                payment.getId(),
                providerResponse.getProviderTransactionId(),
                providerResponse.getStatus()
        );

        return providerResponse;
    }

    /*
     * Проверяем семантику idempotency key.
     *
     * Один internalTransactionUid может повторно использоваться
     * только с теми же:
     *
     * - amount
     * - currency
     * - methodId
     *
     * Иначе это уже другая бизнес-операция
     * под тем же idempotency key.
     */
    private void validateIdempotentRequest(
            Payment existingPayment,
            PaymentRequest request
    ) {

        /*
         * BigDecimal нельзя сравнивать через equals(),
         * потому что:
         *
         * 100.5 != 100.50 через equals()
         *
         * Но денежное значение у них одинаковое.
         */
        boolean sameAmount =
                existingPayment
                        .getAmount()
                        .compareTo(
                                request.getAmount()
                        ) == 0;

        boolean sameCurrency =
                existingPayment
                        .getCurrency()
                        .equals(
                                request.getCurrency()
                        );

        boolean sameMethod =
                existingPayment
                        .getPaymentMethod()
                        .getId()
                        .longValue()
                        == request
                        .getMethodId()
                        .longValue();

        if (!sameAmount
                || !sameCurrency
                || !sameMethod) {

            log.warn(
                    "Idempotency conflict detected. " +
                            "internalTransactionId={}, " +
                            "existingMethodId={}, requestMethodId={}, " +
                            "existingAmount={}, requestAmount={}, " +
                            "existingCurrency={}, requestCurrency={}",
                    existingPayment.getInternalTransactionId(),
                    existingPayment.getPaymentMethod().getId(),
                    request.getMethodId(),
                    existingPayment.getAmount(),
                    request.getAmount(),
                    existingPayment.getCurrency(),
                    request.getCurrency()
            );

            throw new PaymentIdempotencyConflictException(
                    existingPayment.getInternalTransactionId()
            );
        }
    }
}