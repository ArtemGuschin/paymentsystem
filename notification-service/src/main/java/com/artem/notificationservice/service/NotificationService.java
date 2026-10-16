package com.artem.notificationservice.service;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.common.notification.dto.NotificationResponse;
import com.artem.common.notification.dto.NotificationStatus;
import com.artem.notificationservice.email.EmailService;
import com.artem.notificationservice.exception.NotificationNotFoundException;
import com.artem.notificationservice.model.NotificationEntity;
import com.artem.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String USER_REGISTERED = "USER_REGISTERED";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserUid(String userUid) {
        return notificationRepository
                .findAllByUserUidOrderByCreatedAtDesc(userUid)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse completeNotification(Long id) {
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        if (notification.getNotificationStatus() == NotificationStatus.COMPLETED) {
            return notificationMapper.toResponse(notification);
        }

        notification.setNotificationStatus(NotificationStatus.COMPLETED);

        NotificationEntity updatedNotification =
                notificationRepository.saveAndFlush(notification);

        return notificationMapper.toResponse(updatedNotification);
    }

    @Transactional
    public void process(NotificationCreated event) {
        String eventId = event.getEventId();

        if (notificationRepository.existsByEventId(eventId)) {
            log.info(
                    "Notification event already processed, skipping: eventId={}",
                    eventId
            );
            return;
        }

        NotificationEntity notification = new NotificationEntity();

        notification.setEventId(eventId);
        notification.setMessage(event.getMessage());
        notification.setUserUid(event.getUserUid());
        notification.setNotificationStatus(NotificationStatus.NEW);
        notification.setSubject(event.getSubject());
        notification.setCreatedBy(event.getCreatedBy());
        notification.setNotificationType(event.getNotificationType());
        notification.setRecipientEmail(event.getEmail());

        notificationRepository.saveAndFlush(notification);

        if (USER_REGISTERED.equals(event.getNotificationType())) {
            sendRegistrationEmail(notification);
        }

        log.info(
                "Notification processed: eventId={}, userUid={}, type={}",
                eventId,
                event.getUserUid(),
                event.getNotificationType()
        );
    }

    private void sendRegistrationEmail(NotificationEntity notification) {
        String recipientEmail = notification.getRecipientEmail();

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalStateException(
                    "Recipient email is required for USER_REGISTERED notification"
            );
        }

        emailService.sendRegistrationEmail(
                recipientEmail,
                notification.getSubject(),
                notification.getMessage()
        );

        notification.setEmailSentAt(LocalDateTime.now());
        notificationRepository.saveAndFlush(notification);

        log.info(
                "Registration email sent: eventId={}, recipient={}",
                notification.getEventId(),
                recipientEmail
        );
    }
}