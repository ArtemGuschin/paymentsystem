package com.artem.paymentservice.repository;


import com.artem.paymentservice.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentProviderRepository
        extends JpaRepository<PaymentProvider, Integer> {

    Optional<PaymentProvider> findByName(String name);

}