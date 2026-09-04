package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.provider.PaymentGateway;
import com.artem.paymentservice.provider.factory.PaymentProviderFactory;
import com.artem.paymentservice.repository.PaymentRepository;
import com.artem.paymentservice.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl
        implements PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentStateService paymentStateService;

    @Override
    public void reconcilePendingPayments() {

        List<Payment> pendingPayments =
                paymentRepository
                        .findByStatusAndExternalTransactionIdIsNotNull(
                                PaymentStatus.PENDING.name()
                        );

        log.info(
                "Starting payment reconciliation. pendingPayments={}",
                pendingPayments.size()
        );

        for (Payment payment : pendingPayments) {

            reconcilePayment(payment);
        }
    }

    private void reconcilePayment(Payment payment) {

        try {

            String providerName =
                    payment.getPaymentMethod()
                            .getProvider()
                            .getName();

            PaymentGateway paymentGateway =
                    paymentProviderFactory.getProvider(
                            providerName
                    );

            PaymentStatus providerStatus =
                    paymentGateway.getPaymentStatus(
                            payment.getExternalTransactionId()
                    );

            log.info(
                    "Payment reconciliation result. " +
                            "paymentId={}, providerTransactionId={}, providerStatus={}",
                    payment.getId(),
                    payment.getExternalTransactionId(),
                    providerStatus
            );

            switch (providerStatus) {

                case SUCCESS -> {

                    paymentStateService.updateStatus(
                            payment,
                            payment.getExternalTransactionId(),
                            PaymentStatus.SUCCESS
                    );

                    log.info(
                            "Payment reconciled to SUCCESS. paymentId={}",
                            payment.getId()
                    );
                }

                case FAILED -> {

                    paymentStateService.updateStatus(
                            payment,
                            payment.getExternalTransactionId(),
                            PaymentStatus.FAILED
                    );

                    log.info(
                            "Payment reconciled to FAILED. paymentId={}",
                            payment.getId()
                    );
                }

                case PENDING -> {

                    log.info(
                            "Payment is still PENDING. paymentId={}, providerTransactionId={}",
                            payment.getId(),
                            payment.getExternalTransactionId()
                    );
                }
            }

        } catch (Exception ex) {

            /*
             * Очень важно:
             *
             * Ошибка reconciliation не означает FAILED.
             * Мы просто не смогли узнать результат.
             *
             * Оставляем Payment = PENDING.
             */
            log.warn(
                    "Could not reconcile payment. " +
                            "paymentId={}, providerTransactionId={}. " +
                            "Keeping PENDING.",
                    payment.getId(),
                    payment.getExternalTransactionId(),
                    ex
            );
        }
    }
}