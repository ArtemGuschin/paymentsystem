
package com.artem.notificationservice.email;

import com.artem.notificationservice.model.EmailDeliveryStatus;
import com.artem.notificationservice.model.NotificationEntity;
import com.artem.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailDeliveryPublisherTest {

    private NotificationRepository repository;
    private EmailService emailService;
    private JdbcTemplate jdbcTemplate;
    private EmailDeliveryPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        emailService = mock(EmailService.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        publisher = new EmailDeliveryPublisher(
                repository,
                emailService,
                jdbcTemplate
        );
    }

    @Test
    void shouldSendEmailSuccessfully() {
        NotificationEntity notification = createNotification(0);

        prepareClaim(notification);

        when(jdbcTemplate.update(
                anyString(),
                any(Object[].class)
        )).thenReturn(1);

        publisher.sendPendingEmails();

        verify(emailService).sendRegistrationEmail(
                "user@example.com",
                "Welcome",
                "Welcome to Payment System"
        );

        verify(jdbcTemplate).update(
                contains("email_delivery_status = 'SENT'"),
                any(Object[].class)
        );
    }

    @Test
    void shouldScheduleRetryWhenSmtpFails() {
        NotificationEntity notification = createNotification(0);

        prepareClaim(notification);

        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendRegistrationEmail(
                        anyString(),
                        anyString(),
                        anyString()
                );

        publisher.sendPendingEmails();

        verify(jdbcTemplate).update(
                contains("email_attempt_count = ?"),
                eq("PENDING"),
                eq(1),
                any(LocalDateTime.class),
                contains("SMTP unavailable"),
                any(LocalDateTime.class),
                eq(7L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldMarkEmailAsFailedAfterTenAttempts() {
        NotificationEntity notification = createNotification(9);

        prepareClaim(notification);

        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendRegistrationEmail(
                        anyString(),
                        anyString(),
                        anyString()
                );

        publisher.sendPendingEmails();

        verify(jdbcTemplate).update(
                contains("email_attempt_count = ?"),
                eq("FAILED"),
                eq(10),
                isNull(),
                contains("SMTP unavailable"),
                any(LocalDateTime.class),
                eq(7L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldNotSendEmailWhenClaimFails() {

        when(repository.findDueDeliveryIds(
                anyCollection(),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(7L));

        when(repository.claimEmailDelivery(
                eq(7L),
                anyCollection(),
                eq(EmailDeliveryStatus.SENDING),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        publisher.sendPendingEmails();

        verifyNoInteractions(emailService);
        verifyNoInteractions(jdbcTemplate);

        verify(repository, never()).findById(anyLong());
    }

    private void prepareClaim(NotificationEntity notification) {

        when(repository.findDueDeliveryIds(
                anyCollection(),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(7L));

        when(repository.claimEmailDelivery(
                eq(7L),
                anyCollection(),
                eq(EmailDeliveryStatus.SENDING),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(repository.findById(7L))
                .thenReturn(Optional.of(notification));
    }

    private NotificationEntity createNotification(int attempts) {

        NotificationEntity notification = new NotificationEntity();

        notification.setId(7L);
        notification.setEventId(
                "11111111-1111-1111-1111-111111111111"
        );

        notification.setNotificationType("USER_REGISTERED");
        notification.setRecipientEmail("user@example.com");
        notification.setSubject("Welcome");
        notification.setMessage("Welcome to Payment System");

        notification.setEmailDeliveryStatus(
                EmailDeliveryStatus.SENDING
        );

        notification.setEmailAttemptCount(attempts);

        return notification;
    }
}
