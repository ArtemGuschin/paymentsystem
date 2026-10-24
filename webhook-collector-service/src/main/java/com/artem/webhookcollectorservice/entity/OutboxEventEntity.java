
package com.artem.webhookcollectorservice.entity;

import com.artem.webhookcollectorservice.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid", nullable = false, updatable = false)
    private UUID uid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public OutboxEventEntity(
            UUID aggregateId,
            String eventType,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.NEW;
        this.createdAt = LocalDateTime.now();
        this.attemptCount = 0;
        this.nextAttemptAt = this.createdAt;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void recordPublishFailure(
            String error,
            LocalDateTime retryAt,
            int maxAttempts
    ) {
        this.attemptCount++;
        this.lastError = error;

        if (this.attemptCount >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.nextAttemptAt = retryAt;
        }
    }
}
