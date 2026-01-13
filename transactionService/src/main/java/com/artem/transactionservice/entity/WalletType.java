package com.artem.transactionservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Entity
@Table(name = "wallet_types")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WalletType {

    @Id
    @GeneratedValue
    private UUID uid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 18)
    private String status;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "user_type", length = 15)
    private String userType;

    @Column(length = 255)
    private String creator;

    @Column(length = 255)
    private String modifier;


}