package com.artem.paymentservice.repository;


import com.artem.paymentservice.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodRepository
        extends JpaRepository<PaymentMethod, Integer> {

    List<PaymentMethod> findByActiveTrue();

}