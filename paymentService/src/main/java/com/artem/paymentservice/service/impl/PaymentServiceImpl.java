package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentRepository;
import com.artem.paymentservice.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.dto.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;


    @Override
    public PaymentResponse processPayment(
            PaymentRequest request
    ) {

        PaymentMethod paymentMethod =
                paymentMethodRepository
                        .findById(request.getMethodId().intValue())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment method not found"
                                )
                        );

        Payment payment = Payment.builder()
                .paymentMethod(paymentMethod)
                .internalTransactionId(
                        request.getInternalTransactionUid().toString()
                )
                .amount(
                        BigDecimal.valueOf(request.getAmount())
                )
                .currency(request.getCurrency())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        return new PaymentResponse()
                .providerTransactionId(
                        savedPayment.getId().toString()
                )
                .status(PaymentStatus.PENDING);
    }
}
