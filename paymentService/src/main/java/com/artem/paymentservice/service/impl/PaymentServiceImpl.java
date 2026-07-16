package com.artem.paymentservice.service.impl;

import com.artem.fakepaymentprovider.client.api.TransactionsApi;
import com.artem.fakepaymentprovider.client.dto.Transaction;
import com.artem.fakepaymentprovider.client.dto.TransactionRequest;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.PaymentStatus;
import com.artem.paymentservice.mapper.TransactionMapper;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentRepository;
import com.artem.paymentservice.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionsApi transactionsApi;
    private final TransactionMapper transactionMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {

        PaymentMethod paymentMethod =
                paymentMethodRepository
                        .findById(request.getMethodId().intValue())
                        .orElseThrow(() ->
                                new RuntimeException("Payment method not found"));

        TransactionRequest transactionRequest =
                transactionMapper.toTransactionRequest(
                        request,
                        paymentMethod.getProviderMethodType()
                );

        Transaction providerTransaction =
                transactionsApi.createTransaction(transactionRequest);

        Payment payment = Payment.builder()
                .paymentMethod(paymentMethod)
                .externalTransactionId(
                        providerTransaction.getId().toString()
                )
                .internalTransactionId(
                        request.getInternalTransactionUid().toString()
                )
                .amount(
                        BigDecimal.valueOf(request.getAmount())
                )
                .currency(request.getCurrency())
                .status(
                        providerTransaction.getStatus().name()
                )
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return new PaymentResponse()
                .providerTransactionId(
                        providerTransaction.getId().toString()
                )
                .status(
                        PaymentStatus.valueOf(
                                providerTransaction.getStatus().name()
                        )
                );
    }
}