package com.artem.transactionservice.entity;


import jakarta.persistence.*;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    private UUID uid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "user_uid",updatable = false, nullable = false)
    private UUID userUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_uid",updatable = false, nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 256)
    private String comment;

    @Column
    private BigDecimal fee;

    @Column(name = "target_wallet_uid")
    private UUID targetWalletUid;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "failure_reason", length = 256)
    private String failureReason;



}