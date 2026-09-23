package com.artem.paymentservice.repository;

import com.artem.paymentservice.model.PaymentMethodDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentMethodDefinitionRepository
        extends JpaRepository<PaymentMethodDefinition, Integer> {

    List<PaymentMethodDefinition> findByIsActiveTrue();

    List<PaymentMethodDefinition>
    findByCurrencyCodeAndCountryAlpha3CodeAndIsActiveTrue(
            String currencyCode,
            String countryAlpha3Code
    );

    @Query("""
            select d
            from PaymentMethodDefinition d
            join fetch d.paymentMethod pm
            where d.isActive = true
              and pm.active = true
              and (d.isAllCurrencies = true or d.currencyCode = :currency)
              and (d.isAllCountries = true or d.countryAlpha3Code = :country)
            order by d.isPriority desc, d.id asc
            """)
    List<PaymentMethodDefinition> findEligibleDefinitions(
            @Param("currency") String currency,
            @Param("country") String country
    );
}