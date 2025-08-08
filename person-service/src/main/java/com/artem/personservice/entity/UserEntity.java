package com.artem.personservice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "person")
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private String secretKey;
    private String email;

    @Column(nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    private String firstName;
    private String lastName;
    private boolean filled;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private AddressEntity address;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IndividualEntity individual;

    @PreUpdate
    public void preUpdate() {
        this.updated = LocalDateTime.now();
    }
}
