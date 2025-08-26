package com.artem.personservice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "individuals", schema = "person")
@Getter
@Setter
@Audited
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