package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentStateService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPendingPayment(
            PaymentMethod paymentMethod,
            String internalTransactionId,
            BigDecimal amount,
            String currency
    ) {

        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .paymentMethod(paymentMethod)
                .internalTransactionId(internalTransactionId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING.name())
                .createdAt(now)
                .modifiedAt(now)
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Payment payment) {

        payment.setStatus(
                PaymentStatus.FAILED.name()
        );

        payment.setModifiedAt(
                LocalDateTime.now()
        );

        paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(
            Payment payment,
            String externalTransactionId,
            PaymentStatus status
    ) {

        payment.setExternalTransactionId(
                externalTransactionId
        );

        payment.setStatus(
                status.name()
        );

        payment.setModifiedAt(
                LocalDateTime.now()
        );

        paymentRepository.save(payment);
    }
}
