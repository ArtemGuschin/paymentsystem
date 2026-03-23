package com.artem.currencyrateservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversion_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_code", referencedColumnName = "code", nullable = false)
    private Currency sourceCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_code", referencedColumnName = "code", nullable = false)
    private Currency destinationCurrency;

    @Column(name = "rate_begin_time", nullable = false)
    private LocalDateTime rateBeginTime;

    @Column(name = "rate_end_time", nullable = false)
    private LocalDateTime rateEndTime;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_code")
    private RateProvider provider;
}