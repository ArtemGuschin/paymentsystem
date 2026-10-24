
package com.artem.webhookcollectorservice.integration;

import com.artem.webhookcollectorservice.TestcontainersConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.outbox.OutboxPublisher;
import com.artem.webhookcollectorservice.outbox.OutboxStatus;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "outbox.publisher.delay-ms=3600000"
})
@Import(TestcontainersConfiguration.class)
class OutboxPublisherStarvationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxPublisher publisher;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldPublishPaymentDespite100FailedNotifications() {

        // 1. Создаём 100 уведомлений с невалидным JSON.
        List<OutboxEventEntity> invalidNotifications =
                new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            invalidNotifications.add(
                    new OutboxEventEntity(
                            UUID.randomUUID(),
                            "notification.created",
                            "invalid-json"
                    )
            );
        }

        repository.saveAllAndFlush(invalidNotifications);

        // 2. Создаём платёжное событие после уведомлений.
        OutboxEventEntity paymentEvent =
                repository.saveAndFlush(
                        new OutboxEventEntity(
                                UUID.randomUUID(),
                                "payment.status.updated",
                                "{\"status\":\"SUCCESS\"}"
                        )
                );

        // 3. Проверяем обработку ошибочных уведомлений.
        publisher.publishNotificationEvents();

        // 4. Платёж должен публиковаться независимо.
        publisher.publishPaymentEvents();

        // 5. Проверяем платёж в PostgreSQL.
        OutboxEventEntity savedPayment =
                repository.findById(paymentEvent.getUid())
                        .orElseThrow();

        assertThat(savedPayment.getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);

        assertThat(savedPayment.getPublishedAt())
                .isNotNull();

        // 6. Проверяем сохранение состояния ошибок.
        List<OutboxEventEntity> notifications =
                repository.findAll()
                        .stream()
                        .filter(event ->
                                "notification.created".equals(
                                        event.getEventType()
                                )
                        )
                        .toList();

        assertThat(notifications).hasSize(100);

        assertThat(notifications).allSatisfy(event -> {
            assertThat(event.getStatus())
                    .isEqualTo(OutboxStatus.NEW);

            assertThat(event.getAttemptCount())
                    .isEqualTo(1);

            assertThat(event.getLastError())
                    .isNotBlank();

            assertThat(event.getNextAttemptAt())
                    .isAfter(event.getCreatedAt());
        });
    }


    @Test
    void shouldMarkNotificationAsFailedAfterTenAttempts() {

        // Создаём одно невалидное уведомление.
        OutboxEventEntity event = repository.saveAndFlush(
                new OutboxEventEntity(
                        UUID.randomUUID(),
                        "notification.created",
                        "invalid-json"
                )
        );

        UUID eventId = event.getUid();

        // Имитируем десять неудачных попыток.
        for (int attempt = 1; attempt <= 10; attempt++) {

            // Ускоряем наступление retry, чтобы не ждать.
            jdbcTemplate.update(
                    """
                    UPDATE outbox_events
                    SET next_attempt_at =
                        CURRENT_TIMESTAMP - INTERVAL '1 minute'
                    WHERE uid = ?
                    """,
                    eventId
            );

            publisher.publishNotificationEvents();
        }

        OutboxEventEntity result =
                repository.findById(eventId).orElseThrow();

        assertThat(result.getAttemptCount()).isEqualTo(10);
        assertThat(result.getStatus())
                .isEqualTo(OutboxStatus.FAILED);

        assertThat(result.getLastError()).isNotBlank();

        // FAILED не должен обрабатываться повторно.
        publisher.publishNotificationEvents();

        OutboxEventEntity afterRetry =
                repository.findById(eventId).orElseThrow();

        assertThat(afterRetry.getAttemptCount()).isEqualTo(10);
    }

}
