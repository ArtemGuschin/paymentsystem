package com.artem.paymentservice.service.impl;

import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.mapper.PaymentMapper;
import com.artem.paymentservice.model.PaymentMethodDefinition;
import com.artem.paymentservice.repository.PaymentMethodDefinitionRepository;
import com.artem.paymentservice.repository.PaymentMethodRequiredFieldRepository;
import com.artem.paymentservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl
        implements PaymentMethodService {

    private final PaymentMethodDefinitionRepository definitionRepository;

    private final PaymentMethodRequiredFieldRepository requiredFieldRepository;

    private final PaymentMapper paymentMapper;

    @Override
    public List<PaymentMethodResponse> getAvailablePaymentMethods(
            String currencyCode,
            String countryCode
    ) {

        List<PaymentMethodDefinition> definitions =
                definitionRepository
                        .findByCurrencyCodeAndCountryAlpha3CodeAndIsActiveTrue(
                                currencyCode,
                                countryCode
                        );

        return definitions.stream()
                .map(PaymentMethodDefinition::getPaymentMethod)
                .map(paymentMapper::toPaymentMethodResponse)
                .toList();
    }
}