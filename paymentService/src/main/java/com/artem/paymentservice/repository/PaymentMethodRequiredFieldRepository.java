package com.artem.paymentservice.repository;


import com.artem.paymentservice.model.PaymentMethodRequiredField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodRequiredFieldRepository
        extends JpaRepository<PaymentMethodRequiredField, UUID> {

    List<PaymentMethodRequiredField>
    findByPaymentMethod_IdAndIsActiveTrue(Integer paymentMethodId);

}