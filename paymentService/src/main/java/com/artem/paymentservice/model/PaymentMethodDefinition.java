package com.artem.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "payment_method_definitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_method_definition",
                        columnNames = {
                                "payment_method_id",
                                "currency_code",
                                "country_alpha3_code"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;
    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "country_alpha3_code")
    private String countryAlpha3Code;

    @Column(name = "is_all_currencies")
    private Boolean isAllCurrencies;

    @Column(name = "is_all_countries")
    private Boolean isAllCountries;

    @Column(name = "is_priority")
    private Boolean isPriority;

    @Column(name = "is_active")
    private Boolean isActive;
}