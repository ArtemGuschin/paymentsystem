package com.artem.individuals.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@Import(NotificationOutboxService.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class NotificationOutboxServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("individuals_outbox_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationOutboxRepository repository;

    @Test
    void shouldNotCreateDuplicateUserRegisteredEvent() {

        UUID userUid = UUID.randomUUID();
        String email = "recovery@test.com";

        // Первая запись.
        String firstEventId =
                outboxService.saveUserRegistered(
                        userUid,
                        email
                );

        // Повторная запись для того же пользователя.
        String secondEventId =
                outboxService.saveUserRegistered(
                        userUid,
                        email
                );

        // EventId должен остаться прежним.
        assertEquals(firstEventId, secondEventId);

        // В PostgreSQL должна существовать одна запись.
        long count = repository.findAll()
                .stream()
                .filter(event ->
                        userUid.equals(event.getUserUid())
                                && "USER_REGISTERED".equals(
                                event.getNotificationType()
                        )
                )
                .count();

        assertEquals(1, count);

        NotificationOutboxEntity event =
                repository.findByUserUidAndNotificationType(
                        userUid,
                        "USER_REGISTERED"
                ).orElseThrow();

        assertEquals(
                NotificationOutboxEntity.Status.NEW,
                event.getStatus()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRecoverOutboxAfterDatabaseWriteFailure() {

        UUID userUid = UUID.randomUUID();
        String email = "recovery-test@example.com";

        // 1. Временно запрещаем запись события
        // для конкретного тестового пользователя.
        jdbcTemplate.execute(
                """
                ALTER TABLE notification_outbox
                ADD CONSTRAINT test_outbox_recovery_failure
                CHECK (user_uid <> '%s'::uuid)
                """.formatted(userUid)
        );

        try {

            // 2. Первая попытка записи завершается ошибкой PostgreSQL.
            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> outboxService.saveUserRegistered(
                            userUid,
                            email
                    )
            );

            // 3. Убеждаемся, что событие не сохранилось.
            assertTrue(
                    repository.findByUserUidAndNotificationType(
                            userUid,
                            "USER_REGISTERED"
                    ).isEmpty()
            );

        } finally {

            // 4. Устраняем временную причину ошибки.
            jdbcTemplate.execute(
                    """
                    ALTER TABLE notification_outbox
                    DROP CONSTRAINT test_outbox_recovery_failure
                    """
            );
        }

        // 5. Повторяем операцию после восстановления записи.
        String recoveredEventId =
                outboxService.saveUserRegistered(userUid, email);

        // 6. Повторный вызов не должен создать дубликат.
        String repeatedEventId =
                outboxService.saveUserRegistered(userUid, email);

        assertEquals(recoveredEventId, repeatedEventId);

        // 7. Проверяем реальное состояние PostgreSQL.
        NotificationOutboxEntity event =
                repository.findByUserUidAndNotificationType(
                        userUid,
                        "USER_REGISTERED"
                ).orElseThrow();

        assertEquals(recoveredEventId, event.getEventId());

        assertEquals(
                NotificationOutboxEntity.Status.NEW,
                event.getStatus()
        );
    }
}