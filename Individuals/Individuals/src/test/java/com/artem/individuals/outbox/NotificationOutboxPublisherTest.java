package com.artem.individuals.outbox;

import com.artem.individuals.kafka.NotificationEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationOutboxPublisherTest {

    private NotificationOutboxRepository repository;
    private NotificationEventProducer producer;
    private NotificationOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        producer = mock(NotificationEventProducer.class);

        publisher = new NotificationOutboxPublisher(
                repository,
                producer
        );
    }

    private NotificationOutboxEntity createEvent() {
        NotificationOutboxEntity event =
                new NotificationOutboxEntity();

        event.setId(UUID.randomUUID());
        event.setEventId(UUID.randomUUID().toString());
        event.setStatus(NotificationOutboxEntity.Status.NEW);
        event.setAttemptCount(0);
        event.setNextAttemptAt(Instant.now().minusSeconds(1));

        return event;
    }

    private void mockPendingEvent(NotificationOutboxEntity event) {
        when(repository
                .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        eq(NotificationOutboxEntity.Status.NEW),
                        any(Instant.class),
                        any(Pageable.class)
                ))
                .thenReturn(List.of(event));
    }

    @Test
    void shouldMarkEventAsPublishedAfterKafkaSuccess() {
        NotificationOutboxEntity event = createEvent();

        mockPendingEvent(event);

        when(producer.sendOutboxEvent(event))
                .thenReturn(Mono.empty());

        publisher.publishPendingEvents();

        assertEquals(
                NotificationOutboxEntity.Status.PUBLISHED,
                event.getStatus()
        );

        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        assertEquals(0, event.getAttemptCount());

        verify(producer).sendOutboxEvent(event);
        verify(repository).saveAndFlush(event);
    }

    @Test
    void shouldScheduleRetryAfterKafkaFailure() {
        NotificationOutboxEntity event = createEvent();

        mockPendingEvent(event);

        when(producer.sendOutboxEvent(event))
                .thenReturn(
                        Mono.error(new RuntimeException("Kafka unavailable"))
                );

        Instant before = Instant.now();

        publisher.publishPendingEvents();

        assertEquals(
                NotificationOutboxEntity.Status.NEW,
                event.getStatus()
        );

        assertEquals(1, event.getAttemptCount());
        assertTrue(event.getNextAttemptAt().isAfter(before));
        assertTrue(event.getLastError().contains("Kafka unavailable"));
        assertNull(event.getPublishedAt());

        verify(repository).saveAndFlush(event);
    }

    @Test
    void shouldMarkEventAsFailedAfterMaxAttempts() {
        NotificationOutboxEntity event = createEvent();
        event.setAttemptCount(9);

        mockPendingEvent(event);

        when(producer.sendOutboxEvent(event))
                .thenReturn(
                        Mono.error(new RuntimeException("Kafka still unavailable"))
                );

        publisher.publishPendingEvents();

        assertEquals(
                NotificationOutboxEntity.Status.FAILED,
                event.getStatus()
        );

        assertEquals(10, event.getAttemptCount());
        assertNotNull(event.getLastError());

        verify(repository).saveAndFlush(event);
    }
}
