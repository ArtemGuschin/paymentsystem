package com.artem.webhookcollectorservice.outbox;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.webhookcollectorservice.client.TransactionNotificationContextClient;
import com.artem.webhookcollectorservice.dto.NotificationContextResponse;
import com.artem.webhookcollectorservice.dto.NotificationOutboxPayload;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.kafka.NotificationEventProducer;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String PAYMENT_STATUS_UPDATED =
            "payment.status.updated";

    private static final String NOTIFICATION_CREATED =
            "notification.created";

    private final OutboxRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final NotificationEventProducer notificationEventProducer;

    private final TransactionNotificationContextClient
            transactionNotificationContextClient;

    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEventEntity> events =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.NEW
                );

        for (OutboxEventEntity event : events) {

            try {

                if (PAYMENT_STATUS_UPDATED.equals(
                        event.getEventType()
                )) {

                    publishPaymentStatusEvent(event);

                } else if (NOTIFICATION_CREATED.equals(
                        event.getEventType()
                )) {

                    publishNotificationEvent(event);

                } else {

                    log.error(
                            "Unsupported outbox event type. eventId={}, eventType={}",
                            event.getUid(),
                            event.getEventType()
                    );

                    continue;
                }

                event.markAsPublished();

                log.info(
                        "Outbox event published. eventId={}, aggregateId={}, eventType={}",
                        event.getUid(),
                        event.getAggregateId(),
                        event.getEventType()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to publish outbox event. eventId={}, eventType={}",
                        event.getUid(),
                        event.getEventType(),
                        e
                );
            }
        }
    }

    private void publishPaymentStatusEvent(
            OutboxEventEntity event
    ) throws Exception {

        kafkaTemplate
                .send(
                        PAYMENT_STATUS_UPDATED,
                        event.getAggregateId().toString(),
                        event.getPayload()
                )
                .get();
    }

    private void publishNotificationEvent(
            OutboxEventEntity event
    ) throws Exception {

        NotificationOutboxPayload payload =
                objectMapper.readValue(
                        event.getPayload(),
                        NotificationOutboxPayload.class
                );

        UUID transactionUid =
                UUID.fromString(
                        payload.transactionUid()
                );

        NotificationContextResponse context =
                transactionNotificationContextClient
                        .getNotificationContext(
                                transactionUid
                        );

        if (context == null
                || context.userUid() == null) {

            throw new IllegalStateException(
                    "Transaction Service returned no userUid for transactionUid="
                            + transactionUid
            );
        }

        NotificationCreated notification =
                NotificationCreated.newBuilder()
                        .setEventId(
                                payload.eventId()
                        )
                        .setNotificationType(
                                payload.notificationType()
                        )
                        .setMessage(
                                payload.message()
                        )
                        .setUserUid(
                                context.userUid().toString()
                        )
                        .setEmail(
                                payload.email()
                        )
                        .setSubject(
                                payload.subject()
                        )
                        .setCreatedBy(
                                payload.createdBy()
                        )
                        .setCreatedAt(
                                Instant.ofEpochMilli(
                                        payload.createdAtEpochMillis()
                                )
                        )
                        .build();

        notificationEventProducer.send(
                NOTIFICATION_CREATED,
                notification.getEventId().toString(),
                notification
        );
    }
}