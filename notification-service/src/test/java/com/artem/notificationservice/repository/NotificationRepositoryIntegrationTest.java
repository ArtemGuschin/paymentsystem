package com.artem.notificationservice.repository;

import com.artem.common.notification.dto.NotificationStatus;
import com.artem.notificationservice.model.NotificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class NotificationRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("notification_test_db")
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

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldSaveAndFindNotificationByUserUid() {
        NotificationEntity notification = createNotification(
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222"
        );

        notificationRepository.saveAndFlush(notification);

        List<NotificationEntity> result =
                notificationRepository
                        .findAllByUserUidOrderByCreatedAtDesc(
                                "22222222-2222-2222-2222-222222222222"
                        );

        assertEquals(1, result.size());

        NotificationEntity saved = result.getFirst();

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        assertEquals(
                NotificationStatus.NEW,
                saved.getNotificationStatus()
        );

        assertEquals(
                "USER_REGISTERED",
                saved.getNotificationType()
        );

        assertEquals(
                "user@example.com",
                saved.getRecipientEmail()
        );
    }

    @Test
    void shouldRejectDuplicateEventId() {
        NotificationEntity first = createNotification(
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444"
        );

        NotificationEntity duplicate = createNotification(
                "33333333-3333-3333-3333-333333333333",
                "55555555-5555-5555-5555-555555555555"
        );

        notificationRepository.saveAndFlush(first);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> notificationRepository.saveAndFlush(duplicate)
        );
    }

    private NotificationEntity createNotification(
            String eventId,
            String userUid
    ) {
        NotificationEntity notification = new NotificationEntity();

        notification.setEventId(eventId);
        notification.setMessage("Welcome to Payment System");
        notification.setUserUid(userUid);
        notification.setNotificationStatus(NotificationStatus.NEW);
        notification.setSubject("Welcome to Payment System");
        notification.setCreatedBy("SYSTEM");
        notification.setNotificationType("USER_REGISTERED");
        notification.setRecipientEmail("user@example.com");

        return notification;
    }
}