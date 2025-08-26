package com.artem.personservice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "person")
@Getter
@Setter
@Audited
public class UserEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private String secretKey;
    private String email;
    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime created = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updated = OffsetDateTime.now();

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
        this.updated = OffsetDateTime.now();
    }
}
