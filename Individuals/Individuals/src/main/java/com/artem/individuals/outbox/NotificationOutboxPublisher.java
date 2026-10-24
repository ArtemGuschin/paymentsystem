
package com.artem.individuals.outbox;

import com.artem.individuals.kafka.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ATTEMPTS = 10;

    private static final Duration SEND_TIMEOUT =
            Duration.ofSeconds(15);

    private final NotificationOutboxRepository repository;
    private final NotificationEventProducer producer;

    @Scheduled(
            fixedDelayString =
                    "${notification.outbox.publisher.delay-ms:5000}"
    )
    public void publishPendingEvents() {

        List<NotificationOutboxEntity> events =
                repository
                        .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                NotificationOutboxEntity.Status.NEW,
                                Instant.now(),
                                PageRequest.of(0, BATCH_SIZE)
                        );

        for (NotificationOutboxEntity event : events) {
            publishOne(event);
        }
    }

    private void publishOne(NotificationOutboxEntity event) {

        try {
            // Ждём подтверждения отправки в Kafka.
            // Метод выполняется в scheduler thread,
            // а не в WebFlux event-loop.
            producer.sendOutboxEvent(event)
                    .block(SEND_TIMEOUT);

            event.setStatus(
                    NotificationOutboxEntity.Status.PUBLISHED
            );
            event.setPublishedAt(Instant.now());
            event.setLastError(null);

            repository.saveAndFlush(event);

            log.info(
                    "Outbox event published: eventId={}",
                    event.getEventId()
            );

        } catch (Exception error) {

            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(error.toString());

            if (attempts >= MAX_ATTEMPTS) {

                event.setStatus(
                        NotificationOutboxEntity.Status.FAILED
                );

                log.error(
                        "Outbox event requires manual recovery: eventId={}, attempts={}",
                        event.getEventId(),
                        attempts,
                        error
                );

            } else {

                long delaySeconds = Math.min(
                        300L,
                        5L * (1L << Math.min(attempts - 1, 6))
                );

                event.setNextAttemptAt(
                        Instant.now().plusSeconds(delaySeconds)
                );

                log.warn(
                        "Outbox publish failed: eventId={}, attempt={}, retryIn={}s",
                        event.getEventId(),
                        attempts,
                        delaySeconds,
                        error
                );
            }

            repository.saveAndFlush(event);
        }
    }
}
