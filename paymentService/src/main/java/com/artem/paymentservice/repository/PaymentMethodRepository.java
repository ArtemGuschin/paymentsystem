package com.artem.paymentservice.repository;

import com.artem.paymentservice.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository
        extends JpaRepository<PaymentMethod, Integer> {

    @Query("""
       select pm
       from PaymentMethod pm
       join PaymentMethodDefinition d on d.paymentMethod = pm
       where pm.id = :methodId
         and pm.active = true
         and d.isActive = true
         and (d.isAllCurrencies = true or d.currencyCode = :currency)
         and (d.isAllCountries = true or d.countryAlpha3Code = :countryCode)
       """)
    Optional<PaymentMethod> findEligibleById(
            @Param("methodId") Integer methodId,
            @Param("currency") String currency,
            @Param("countryCode") String countryCode
    );

    List<PaymentMethod> findByActiveTrue();
}