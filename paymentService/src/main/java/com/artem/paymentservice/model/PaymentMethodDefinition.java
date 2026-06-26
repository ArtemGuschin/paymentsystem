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

    private String currencyCode;

    private String countryAlpha3Code;

    private Boolean isAllCurrencies;

    private Boolean isAllCountries;

    private Boolean isPriority;

    private Boolean isActive;
}