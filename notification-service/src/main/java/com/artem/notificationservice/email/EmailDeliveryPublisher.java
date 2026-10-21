
package com.artem.notificationservice.email;

import com.artem.notificationservice.model.EmailDeliveryStatus;
import com.artem.notificationservice.model.NotificationEntity;
import com.artem.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDeliveryPublisher {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ATTEMPTS = 10;
    private static final int LEASE_MINUTES = 2;

    private static final String USER_REGISTERED =
            "USER_REGISTERED";

    private static final List<EmailDeliveryStatus> CLAIMABLE =
            List.of(
                    EmailDeliveryStatus.PENDING,
                    EmailDeliveryStatus.SENDING
            );

    private final NotificationRepository repository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(
            fixedDelayString =
                    "${app.email.publisher.delay-ms:5000}"
    )
    public void sendPendingEmails() {

        List<Long> ids = repository.findDueDeliveryIds(
                CLAIMABLE,
                LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)
        );

        for (Long id : ids) {
            try {
                sendOne(id);
            } catch (Exception error) {
                log.error(
                        "Unexpected email processing error: id={}",
                        id,
                        error
                );
            }
        }
    }

    private void sendOne(Long id) {

        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);

        LocalDateTime leaseUntil =
                now.plusMinutes(LEASE_MINUTES);

        // Атомарно резервируем письмо.
        int claimed = repository.claimEmailDelivery(
                id,
                CLAIMABLE,
                EmailDeliveryStatus.SENDING,
                now,
                leaseUntil
        );

        if (claimed != 1) {
            return;
        }

        NotificationEntity notification =
                repository.findById(id).orElseThrow();

        // SMTP выполняется без транзакции PostgreSQL.
        try {
            if (!USER_REGISTERED.equals(
                    notification.getNotificationType()
            )) {
                throw new IllegalStateException(
                        "Unsupported email notification type"
                );
            }

            String email = notification.getRecipientEmail();

            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "Recipient email is missing"
                );
            }

            emailService.sendRegistrationEmail(
                    email,
                    notification.getSubject(),
                    notification.getMessage()
            );

        } catch (Exception smtpError) {
            recordFailure(notification, leaseUntil, smtpError);
            return;
        }

        // SMTP завершился успешно.
        // Сохранение SENT — отдельная операция.
        try {
            LocalDateTime sentAt = LocalDateTime.now();

            int updated = jdbcTemplate.update(
                    """
                    UPDATE notifications
                    SET email_delivery_status = 'SENT',
                        email_sent_at = ?,
                        email_next_attempt_at = NULL,
                        email_last_error = NULL,
                        modified_at = ?
                    WHERE id = ?
                      AND email_delivery_status = 'SENDING'
                      AND email_next_attempt_at = ?
                    """,
                    sentAt,
                    sentAt,
                    id,
                    leaseUntil
            );

            if (updated == 1) {
                log.info(
                        "Email sent: id={}, eventId={}",
                        id,
                        notification.getEventId()
                );
            } else {
                log.warn(
                        "Email delivery lease changed before SENT update: id={}",
                        id
                );
            }

        } catch (Exception dbError) {
            log.error(
                    "SMTP accepted email but saving SENT failed: id={}",
                    id,
                    dbError
            );
        }
    }

    private void recordFailure(
            NotificationEntity notification,
            LocalDateTime leaseUntil,
            Exception error
    ) {

        int attempts =
                notification.getEmailAttemptCount() + 1;

        boolean exhausted = attempts >= MAX_ATTEMPTS;

        long delaySeconds = Math.min(
                300L,
                5L * (1L << Math.min(attempts - 1, 6))
        );

        String nextStatus = exhausted
                ? EmailDeliveryStatus.FAILED.name()
                : EmailDeliveryStatus.PENDING.name();

        LocalDateTime retryAt = exhausted
                ? null
                : LocalDateTime.now()
                .plusSeconds(delaySeconds);

        try {
            int updated = jdbcTemplate.update(
                    """
                    UPDATE notifications
                    SET email_delivery_status = ?,
                        email_attempt_count = ?,
                        email_next_attempt_at = ?,
                        email_last_error = ?,
                        modified_at = ?
                    WHERE id = ?
                      AND email_delivery_status = 'SENDING'
                      AND email_next_attempt_at = ?
                    """,
                    nextStatus,
                    attempts,
                    retryAt,
                    error.toString(),
                    LocalDateTime.now(),
                    notification.getId(),
                    leaseUntil
            );

            if (updated == 0) {
                log.warn(
                        "Email delivery lease changed: id={}",
                        notification.getId()
                );
                return;
            }

            if (exhausted) {
                log.error(
                        "Email delivery FAILED: id={}, attempts={}",
                        notification.getId(),
                        attempts,
                        error
                );
            } else {
                log.warn(
                        "Email delivery retry scheduled: id={}, attempt={}, retryIn={}s",
                        notification.getId(),
                        attempts,
                        delaySeconds,
                        error
                );
            }

        } catch (Exception dbError) {
            log.error(
                    "Failed to persist email retry state: id={}",
                    notification.getId(),
                    dbError
            );
        }
    }
}
