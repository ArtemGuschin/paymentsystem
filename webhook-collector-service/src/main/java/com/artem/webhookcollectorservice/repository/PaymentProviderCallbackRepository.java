package com.artem.webhookcollectorservice.repository;

import com.artem.webhookcollectorservice.entity.PaymentProviderCallbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentProviderCallbackRepository
        extends JpaRepository<PaymentProviderCallbackEntity, UUID> {


    @Modifying
    @Query(value = """
            INSERT INTO payment_provider_callbacks (
                uid,
                created_at,
                updated_at,
                body,
                provider_transaction_id,
                type,
                provider
            )
            VALUES (
                :uid,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                :body,
                :providerTransactionId,
                :type,
                :provider
            )
            ON CONFLICT (provider, provider_transaction_id, type)
            DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("uid") UUID uid,
            @Param("body") String body,
            @Param("providerTransactionId") Long providerTransactionId,
            @Param("type") String type,
            @Param("provider") String provider
    );
}