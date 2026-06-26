package com.artem.paymentservice.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 256)
    private String description;
}