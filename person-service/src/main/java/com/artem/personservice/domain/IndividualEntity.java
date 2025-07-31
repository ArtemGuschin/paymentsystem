package com.artem.personservice.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "individuals", schema = "person")
@Getter
@Setter
public class IndividualEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String passportNumber;
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDateTime verifiedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime archivedAt = LocalDateTime.now();

    private String status;
}