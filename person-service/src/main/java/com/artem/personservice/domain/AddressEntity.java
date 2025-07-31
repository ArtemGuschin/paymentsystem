package com.artem.personservice.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "addresses", schema = "person")
@Getter
@Setter
public class AddressEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private CountryEntity country;

    @Column(nullable = false)
    private LocalDateTime archived = LocalDateTime.now();

    private String address;
    private String zipCode;
//    private LocalDateTime archived;
    private String city;
    private String state;

    @PreUpdate
    public void preUpdate() {
        this.updated = LocalDateTime.now();
    }
}