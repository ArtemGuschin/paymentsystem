package com.artem.webhookcollectorservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_provider_callbacks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payment_provider_callback",
                        columnNames = {
                                "provider",
                                "provider_transaction_id",
                                "type"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentProviderCallbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid", nullable = false, updatable = false)
    private UUID uid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Lob
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "provider_transaction_id", nullable = false)
    private Long providerTransactionId;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "provider", nullable = false, length = 255)
    private String provider;

    public PaymentProviderCallbackEntity(
            String body,
            Long providerTransactionId,
            String type,
            String provider
    ) {
        this.body = body;
        this.providerTransactionId = providerTransactionId;
        this.type = type;
        this.provider = provider;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}