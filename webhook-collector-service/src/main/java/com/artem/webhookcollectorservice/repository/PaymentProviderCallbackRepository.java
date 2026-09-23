package com.artem.webhookcollectorservice.repository;

import com.artem.webhookcollectorservice.entity.PaymentProviderCallbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentProviderCallbackRepository
        extends JpaRepository<PaymentProviderCallbackEntity, UUID> {

    boolean existsByProviderAndProviderTransactionIdAndType(
            String provider,
            Long providerTransactionId,
            String type
    );
}