package com.artem.fakepaymentprovider.model;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "webhooks",
        indexes = {
                @Index(name = "idx_webhooks_entity", columnList = "event_type, entity_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "notification_url", length = 2048)
    private String notificationUrl;
}
