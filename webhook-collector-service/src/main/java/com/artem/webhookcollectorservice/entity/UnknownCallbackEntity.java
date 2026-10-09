package com.artem.webhookcollectorservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unknown_callbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnknownCallbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid", nullable = false, updatable = false)
    private UUID uid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    public UnknownCallbackEntity(String body) {
        this.body = body;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}