package com.artem.notificationservice.service;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.common.notification.dto.NotificationResponse;
import com.artem.common.notification.dto.NotificationStatus;
import com.artem.notificationservice.email.EmailService;
import com.artem.notificationservice.exception.NotificationNotFoundException;
import com.artem.notificationservice.model.NotificationEntity;
import com.artem.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private EmailService emailService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        emailService = mock(EmailService.class);

        NotificationMapper notificationMapper = new NotificationMapper();

        notificationService = new NotificationService(
                notificationRepository,
                notificationMapper,
                emailService
        );
    }

    @Test
    void shouldCompleteNewNotification() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setNotificationStatus(NotificationStatus.NEW);

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.saveAndFlush(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response =
                notificationService.completeNotification(1L);

        assertEquals(NotificationStatus.COMPLETED, response.notificationStatus());

        verify(notificationRepository)
                .saveAndFlush(notification);
    }

    @Test
    void shouldNotSaveAgainWhenNotificationAlreadyCompleted() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setNotificationStatus(NotificationStatus.COMPLETED);

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        NotificationResponse response =
                notificationService.completeNotification(1L);

        assertEquals(NotificationStatus.COMPLETED, response.notificationStatus());

        verify(notificationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.completeNotification(999L)
        );

        verify(notificationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldSkipDuplicateEvent() {
        NotificationCreated event = mock(NotificationCreated.class);

        when(event.getEventId())
                .thenReturn("11111111-1111-1111-1111-111111111111");

        when(notificationRepository.existsByEventId(
                "11111111-1111-1111-1111-111111111111"
        )).thenReturn(true);

        notificationService.process(event);

        verify(notificationRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(emailService);
    }

    @Test
    void shouldSaveNotificationAndSendRegistrationEmail() {
        NotificationCreated event = mock(NotificationCreated.class);

        when(event.getEventId())
                .thenReturn("22222222-2222-2222-2222-222222222222");
        when(event.getNotificationType())
                .thenReturn("USER_REGISTERED");
        when(event.getMessage())
                .thenReturn("Welcome to Payment System");
        when(event.getUserUid())
                .thenReturn("33333333-3333-3333-3333-333333333333");
        when(event.getEmail())
                .thenReturn("user@example.com");
        when(event.getSubject())
                .thenReturn("Welcome to Payment System");
        when(event.getCreatedBy())
                .thenReturn("SYSTEM");

        when(notificationRepository.existsByEventId(
                "22222222-2222-2222-2222-222222222222"
        )).thenReturn(false);

        notificationService.process(event);

        verify(emailService).sendRegistrationEmail(
                "user@example.com",
                "Welcome to Payment System",
                "Welcome to Payment System"
        );

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);

        verify(notificationRepository, times(2))
                .saveAndFlush(captor.capture());

        NotificationEntity savedNotification =
                captor.getAllValues().getLast();

        assertEquals(
                NotificationStatus.NEW,
                savedNotification.getNotificationStatus()
        );

        assertEquals(
                "USER_REGISTERED",
                savedNotification.getNotificationType()
        );

        assertEquals(
                "user@example.com",
                savedNotification.getRecipientEmail()
        );

        assertNotNull(savedNotification.getEmailSentAt());
    }

    @Test
    void shouldFailRegistrationNotificationWhenEmailIsMissing() {
        NotificationCreated event = mock(NotificationCreated.class);

        when(event.getEventId())
                .thenReturn("44444444-4444-4444-4444-444444444444");
        when(event.getNotificationType())
                .thenReturn("USER_REGISTERED");
        when(event.getMessage())
                .thenReturn("Welcome");
        when(event.getUserUid())
                .thenReturn("55555555-5555-5555-5555-555555555555");
        when(event.getEmail())
                .thenReturn(null);
        when(event.getSubject())
                .thenReturn("Registration");
        when(event.getCreatedBy())
                .thenReturn("SYSTEM");

        assertThrows(
                IllegalStateException.class,
                () -> notificationService.process(event)
        );

        verifyNoInteractions(emailService);
    }
}