package com.artem.fakepaymentprovider.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 50)
    private String merchantId;

    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}