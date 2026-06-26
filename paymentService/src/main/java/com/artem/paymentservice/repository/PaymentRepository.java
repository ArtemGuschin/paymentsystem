package com.artem.paymentservice.repository;


import com.artem.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByInternalTransactionId(String internalTransactionId);

}