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
import com.artem.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentStateService paymentStateService;

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
         * 1. Находим способ оплаты
         */
        PaymentMethod paymentMethod =
                paymentMethodRepository
                        .findById(request.getMethodId().intValue())
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
         * 2. Создаём Payment со статусом PENDING
         *
         * PaymentStateService выполняет эту операцию
         * в отдельной транзакции REQUIRES_NEW.
         *
         * Поэтому PENDING будет физически зафиксирован
         * в БД ещё до вызова внешнего провайдера.
         */
        Payment payment =
                paymentStateService.createPendingPayment(
                        paymentMethod,
                        request.getInternalTransactionUid().toString(),
                        BigDecimal.valueOf(request.getAmount()),
                        request.getCurrency()
                );

        log.info(
                "Payment created. paymentId={}, internalTransactionId={}, status={}",
                payment.getId(),
                payment.getInternalTransactionId(),
                payment.getStatus()
        );

        /*
         * 3. Получаем нужный PaymentGateway
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
         * 4. Вызываем внешний Payment Provider
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
             * Основной внешний вызов завершился ошибкой.
             *
             * Компенсируем ранее созданный PENDING-платёж:
             *
             * PENDING -> FAILED
             */
            paymentStateService.markFailed(payment);

            throw new PaymentProviderUnavailableException(ex);
        }

        /*
         * 5. Проверяем ответ провайдера
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
         * 6. Обновляем состояние Payment
         *
         * Важно:
         * не изменяем detached entity вручную.
         * PaymentStateService откроет отдельную транзакцию
         * и сохранит актуальное состояние.
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
         * 7. Возвращаем ответ вызывающему сервису
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