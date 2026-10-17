package com.artem.notificationservice.model;

import com.artem.common.notification.dto.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "user_uid", nullable = false, length = 36)
    private String userUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 32)
    private NotificationStatus notificationStatus;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "notification_type", nullable = false, length = 64)
    private String notificationType;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (notificationStatus == null) {
            notificationStatus = NotificationStatus.NEW;
        }
    }

    @PreUpdate
    public void preUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}