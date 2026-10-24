
package com.artem.individuals.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.individuals.outbox.NotificationOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${notification.kafka.topic}")
    private String notificationTopic;

    public Mono<Void> sendOutboxEvent(
            NotificationOutboxEntity outboxEvent
    ) {
        return Mono.defer(() -> {

            NotificationCreated event = NotificationCreated.newBuilder()
                    .setEventId(outboxEvent.getEventId())
                    .setNotificationType(outboxEvent.getNotificationType())
                    .setMessage(outboxEvent.getMessage())
                    .setUserUid(outboxEvent.getUserUid().toString())
                    .setEmail(outboxEvent.getEmail())
                    .setSubject(outboxEvent.getSubject())
                    .setCreatedBy(outboxEvent.getCreatedBy())
                    .setCreatedAt(outboxEvent.getEventCreatedAt())
                    .build();

            return Mono.fromFuture(
                            kafkaTemplate.send(
                                    notificationTopic,
                                    event.getEventId().toString(),
                                    event
                            )
                    )
                    .doOnSuccess(result ->
                            log.info(
                                    "Outbox event published to Kafka: eventId={}, userUid={}",
                                    event.getEventId(),
                                    event.getUserUid()
                            )
                    )
                    .doOnError(error ->
                            log.error(
                                    "Failed to publish Outbox event: eventId={}",
                                    event.getEventId(),
                                    error
                            )
                    )
                    .then();
        });
    }
}
