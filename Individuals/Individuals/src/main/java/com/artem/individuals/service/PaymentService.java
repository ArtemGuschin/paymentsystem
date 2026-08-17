package com.artem.individuals.service;

import com.artem.individuals.client.PaymentClient;
import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.response.PaymentMethodResponseDto;
import com.artem.individuals.dto.response.PaymentResponseDto;
import com.artem.individuals.mapper.PaymentMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;
    private final PaymentMapper paymentMapper;

    /**
     * Получение доступных способов оплаты
     */
    public Flux<PaymentMethodResponseDto> getAvailablePaymentMethods(
            String currencyCode,
            String countryCode) {

        return paymentClient
                .getAvailablePaymentMethods(currencyCode, countryCode)
                .map(paymentMapper::toPaymentMethodResponseDto);
    }

    /**
     * Выполнение платежа
     */
    public Mono<PaymentResponseDto> processPayment(
            PaymentRequestDto dto) {

        return paymentClient
                .processPayment(paymentMapper.toPaymentRequest(dto))
                .map(paymentMapper::toPaymentResponseDto);
    }
}