package com.artem.currencyrateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "code", unique = true, length = 3)
    private String code;
    @Column(name = "iso_code", nullable = false, unique = true)
    private Integer isoCode;
    @Column(name = "description", nullable = false, length = 64)
    private String description;
    @Column(nullable = false)
    private Boolean active;
    @Column(length = 2)
    private String symbol;
}
