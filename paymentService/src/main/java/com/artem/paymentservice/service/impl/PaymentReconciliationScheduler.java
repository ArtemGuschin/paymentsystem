package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean enabled;

    @Scheduled(
            fixedDelayString =
                    "${payment.reconciliation.fixed-delay:30000}"
    )
    public void reconcilePayments() {

        if (!enabled) {
            return;
        }

        reconciliationService.reconcilePendingPayments();
    }
}