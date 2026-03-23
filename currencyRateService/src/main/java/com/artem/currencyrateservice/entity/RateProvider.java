package com.artem.currencyrateservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateProvider {

    @Id
    @Column(name = "provider_code", length = 3)
    private String providerCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "provider_name", nullable = false, unique = true, length = 28)
    private String providerName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean active;
}