
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

import java.time.Instant;
import java.time.LocalDateTime;
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

    private static final int MAX_ATTEMPTS = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationEventProducer notificationEventProducer;
    private final TransactionNotificationContextClient
            transactionNotificationContextClient;
    private final ObjectMapper objectMapper;

    @Scheduled(
            fixedDelayString = "${outbox.publisher.delay-ms:5000}"
    )
    public void publishPaymentEvents() {
        publishBatch(PAYMENT_STATUS_UPDATED);
    }

    @Scheduled(
            fixedDelayString = "${outbox.publisher.delay-ms:5000}"
    )
    public void publishNotificationEvents() {
        publishBatch(NOTIFICATION_CREATED);
    }

    private void publishBatch(String eventType) {

        List<OutboxEventEntity> events =
                outboxRepository
                        .findTop100ByStatusAndEventTypeAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                OutboxStatus.NEW,
                                eventType,
                                LocalDateTime.now()
                        );

        for (OutboxEventEntity event : events) {

            try {
                if (PAYMENT_STATUS_UPDATED.equals(eventType)) {
                    publishPaymentStatusEvent(event);
                } else {
                    publishNotificationEvent(event);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn(
                        "Outbox publisher interrupted: eventId={}",
                        event.getUid(),
                        e
                );
                return;

            } catch (Exception e) {
                recordFailure(event, e);
                continue;
            }

            // Kafka подтвердила отправку.
            // Сохраняем результат отдельной транзакцией Repository.
            event.markAsPublished();

            try {
                outboxRepository.saveAndFlush(event);

                log.info(
                        "Outbox event published: eventId={}, eventType={}",
                        event.getUid(),
                        eventType
                );

            } catch (Exception e) {
                // Kafka уже могла принять событие.
                // Нельзя считать это ошибкой Kafka.
                log.error(
                        "Kafka accepted event, but DB update failed: eventId={}",
                        event.getUid(),
                        e
                );
            }
        }
    }

    private void recordFailure(
            OutboxEventEntity event,
            Exception error
    ) {

        int nextAttempt = event.getAttemptCount() + 1;

        long delaySeconds = Math.min(
                300L,
                5L * (1L << Math.min(nextAttempt - 1, 6))
        );

        LocalDateTime retryAt =
                LocalDateTime.now().plusSeconds(delaySeconds);

        event.recordPublishFailure(
                error.toString(),
                retryAt,
                MAX_ATTEMPTS
        );

        try {
            outboxRepository.saveAndFlush(event);

            if (event.getStatus() == OutboxStatus.FAILED) {
                log.error(
                        "Outbox event requires manual recovery: eventId={}, attempts={}",
                        event.getUid(),
                        event.getAttemptCount(),
                        error
                );
            } else {
                log.warn(
                        "Outbox publish failed: eventId={}, attempt={}, retryIn={}s",
                        event.getUid(),
                        event.getAttemptCount(),
                        delaySeconds,
                        error
                );
            }

        } catch (Exception dbError) {
            log.error(
                    "Failed to save Outbox retry state: eventId={}",
                    event.getUid(),
                    dbError
            );
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
                UUID.fromString(payload.transactionUid());

        NotificationContextResponse context =
                transactionNotificationContextClient
                        .getNotificationContext(transactionUid);

        if (context == null || context.userUid() == null) {
            throw new IllegalStateException(
                    "Transaction Service returned no userUid for transactionUid="
                            + transactionUid
            );
        }

        NotificationCreated notification =
                NotificationCreated.newBuilder()
                        .setEventId(payload.eventId())
                        .setNotificationType(payload.notificationType())
                        .setMessage(payload.message())
                        .setUserUid(context.userUid().toString())
                        .setEmail(payload.email())
                        .setSubject(payload.subject())
                        .setCreatedBy(payload.createdBy())
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
