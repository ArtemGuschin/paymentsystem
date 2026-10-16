package com.artem.individuals.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private static final String USER_REGISTERED = "USER_REGISTERED";
    private static final String SYSTEM = "SYSTEM";

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${notification.kafka.topic}")
    private String notificationTopic;

    public Mono<Void> sendUserRegisteredNotification(
            UUID userUid,
            String email
    ) {
        NotificationCreated event = NotificationCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setNotificationType(USER_REGISTERED)
                .setMessage("Welcome to Payment System")
                .setUserUid(userUid.toString())
                .setEmail(email)
                .setSubject("Welcome to Payment System")
                .setCreatedBy(SYSTEM)
                .setCreatedAt(Instant.now())
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
                                "USER_REGISTERED notification event published: eventId={}, userUid={}",
                                event.getEventId(),
                                event.getUserUid()
                        )
                )
                .doOnError(error ->
                        log.error(
                                "Failed to publish USER_REGISTERED notification: eventId={}, userUid={}",
                                event.getEventId(),
                                event.getUserUid(),
                                error
                        )
                )
                .then();
    }
}