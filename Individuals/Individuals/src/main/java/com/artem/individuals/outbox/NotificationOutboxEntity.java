
package com.artem.individuals.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxEntity {

    public enum Status {
        NEW,
        PUBLISHED,
        FAILED
    }

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "user_uid", nullable = false)
    private UUID userUid;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "notification_type", nullable = false, length = 64)
    private String notificationType;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "event_created_at", nullable = false)
    private Instant eventCreatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void beforeInsert() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}

