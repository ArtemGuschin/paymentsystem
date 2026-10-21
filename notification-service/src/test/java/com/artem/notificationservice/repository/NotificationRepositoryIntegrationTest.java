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
import com.artem.notificationservice.model.EmailDeliveryStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldClaimAndRecoverExpiredEmailLease() {

        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);

        NotificationEntity notification = createNotification(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        notification.setEmailDeliveryStatus(
                EmailDeliveryStatus.PENDING
        );
        notification.setEmailNextAttemptAt(
                now.minusSeconds(1)
        );

        Long id = notificationRepository
                .saveAndFlush(notification)
                .getId();

        List<EmailDeliveryStatus> claimable = List.of(
                EmailDeliveryStatus.PENDING,
                EmailDeliveryStatus.SENDING
        );

        assertTrue(
                notificationRepository.findDueDeliveryIds(
                        claimable,
                        now,
                        PageRequest.of(0, 20)
                ).contains(id)
        );

        LocalDateTime leaseUntil = now.plusMinutes(2);

        int firstClaim = notificationRepository.claimEmailDelivery(
                id,
                claimable,
                EmailDeliveryStatus.SENDING,
                now,
                leaseUntil
        );

        int secondClaim = notificationRepository.claimEmailDelivery(
                id,
                claimable,
                EmailDeliveryStatus.SENDING,
                now,
                leaseUntil
        );

        assertEquals(1, firstClaim);
        assertEquals(0, secondClaim);

        NotificationEntity claimed = notificationRepository
                .findById(id)
                .orElseThrow();

        assertEquals(
                EmailDeliveryStatus.SENDING,
                claimed.getEmailDeliveryStatus()
        );

        assertEquals(
                leaseUntil,
                claimed.getEmailNextAttemptAt()
        );

        LocalDateTime afterExpiry = leaseUntil.plusSeconds(1);

        int recovered = notificationRepository.claimEmailDelivery(
                id,
                claimable,
                EmailDeliveryStatus.SENDING,
                afterExpiry,
                afterExpiry.plusMinutes(2)
        );

        assertEquals(1, recovered);
    }



    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAllowOnlyOneConcurrentEmailClaim() throws Exception {

        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);

        NotificationEntity notification = createNotification(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        notification.setEmailDeliveryStatus(
                EmailDeliveryStatus.PENDING
        );
        notification.setEmailNextAttemptAt(
                now.minusSeconds(1)
        );

        Long id = notificationRepository
                .saveAndFlush(notification)
                .getId();

        List<EmailDeliveryStatus> claimable = List.of(
                EmailDeliveryStatus.PENDING,
                EmailDeliveryStatus.SENDING
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        Callable<Integer> claimTask = () -> {

            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for concurrent start"
                );
            }

            return notificationRepository.claimEmailDelivery(
                    id,
                    claimable,
                    EmailDeliveryStatus.SENDING,
                    now,
                    now.plusMinutes(2)
            );
        };

        try {
            Future<Integer> first = executor.submit(claimTask);
            Future<Integer> second = executor.submit(claimTask);

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both workers must be ready"
            );

            // Оба потока начинают попытку захвата.
            start.countDown();

            int firstResult = first.get(15, TimeUnit.SECONDS);
            int secondResult = second.get(15, TimeUnit.SECONDS);

            assertEquals(
                    1,
                    firstResult + secondResult,
                    "Only one worker should claim the email"
            );

            NotificationEntity claimed =
                    notificationRepository.findById(id)
                            .orElseThrow();

            assertEquals(
                    EmailDeliveryStatus.SENDING,
                    claimed.getEmailDeliveryStatus()
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }




}