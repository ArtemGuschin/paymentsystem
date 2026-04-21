package com.artem.fakepaymentprovider.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_merchant_date", columnList = "merchant_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private MerchantEntity merchant;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "method", nullable = false, length = 50)
    private String method;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "notification_url", length = 2048)
    private String notificationUrl;
}