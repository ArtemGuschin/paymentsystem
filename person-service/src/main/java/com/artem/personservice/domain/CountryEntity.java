package com.artem.personservice.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "countries", schema = "person")
@Getter
@Setter
public class CountryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDateTime created = LocalDateTime.now();
    private LocalDateTime updated = LocalDateTime.now();
    private String name;
    private String alpha2;
    private String alpha3;
    private String status;

    @PreUpdate
    public void preUpdate() {
        this.updated = LocalDateTime.now();
    }
}

