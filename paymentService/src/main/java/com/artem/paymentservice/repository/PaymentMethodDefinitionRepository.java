package com.artem.paymentservice.repository;


import com.artem.paymentservice.model.PaymentMethodDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodDefinitionRepository
        extends JpaRepository<PaymentMethodDefinition, Integer> {

    List<PaymentMethodDefinition> findByIsActiveTrue();
    List<PaymentMethodDefinition>
    findByCurrencyCodeAndCountryAlpha3CodeAndIsActiveTrue(
            String currencyCode,
            String countryAlpha3Code
    );

}