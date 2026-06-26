package com.artem.paymentservice.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "provider_unique_id", nullable = false, unique = true)
    private String providerUniqueId;

    @Column(name = "provider_method_type", nullable = false)
    private String providerMethodType;

    private String logo;

    @Column(name = "profile_type", nullable = false)
    private String profileType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "paymentMethod",
            fetch = FetchType.LAZY
    )
    private List<PaymentMethodRequiredField>
            requiredFields;
}