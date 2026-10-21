
package com.artem.notificationservice.service;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.common.notification.dto.NotificationResponse;
import com.artem.common.notification.dto.NotificationStatus;
import com.artem.notificationservice.exception.NotificationNotFoundException;
import com.artem.notificationservice.model.EmailDeliveryStatus;
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
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);

        NotificationMapper notificationMapper =
                new NotificationMapper();

        notificationService = new NotificationService(
                notificationRepository,
                notificationMapper
        );
    }

    @Test
    void shouldCompleteNewNotification() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setNotificationStatus(NotificationStatus.NEW);

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.saveAndFlush(
                any(NotificationEntity.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response =
                notificationService.completeNotification(1L);

        assertEquals(
                NotificationStatus.COMPLETED,
                response.notificationStatus()
        );

        verify(notificationRepository).saveAndFlush(notification);
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

        assertEquals(
                NotificationStatus.COMPLETED,
                response.notificationStatus()
        );

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
    }

    @Test
    void shouldSaveRegistrationNotificationAsPending() {
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

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);

        notificationService.process(event);

        verify(notificationRepository, times(1))
                .saveAndFlush(captor.capture());

        NotificationEntity savedNotification =
                captor.getValue();

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

        assertEquals(
                EmailDeliveryStatus.PENDING,
                savedNotification.getEmailDeliveryStatus()
        );

        assertNotNull(
                savedNotification.getEmailNextAttemptAt()
        );

        assertNull(
                savedNotification.getEmailSentAt()
        );

        assertEquals(
                0,
                savedNotification.getEmailAttemptCount()
        );
    }

    @Test
    void shouldFailRegistrationNotificationWhenEmailIsMissing() {
        NotificationCreated event = mock(NotificationCreated.class);

        when(event.getEventId())
                .thenReturn("44444444-4444-4444-4444-444444444444");
        when(event.getNotificationType())
                .thenReturn("USER_REGISTERED");
        when(event.getEmail())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> notificationService.process(event)
        );

        verify(notificationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldNotScheduleEmailForOtherNotificationTypes() {
        NotificationCreated event = mock(NotificationCreated.class);

        when(event.getEventId())
                .thenReturn("55555555-5555-5555-5555-555555555555");
        when(event.getNotificationType())
                .thenReturn("PAYMENT_SUCCEEDED");
        when(event.getMessage())
                .thenReturn("Payment completed");
        when(event.getUserUid())
                .thenReturn("66666666-6666-6666-6666-666666666666");
        when(event.getSubject())
                .thenReturn("Payment successful");
        when(event.getCreatedBy())
                .thenReturn("SYSTEM");

        notificationService.process(event);

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);

        verify(notificationRepository)
                .saveAndFlush(captor.capture());

        NotificationEntity savedNotification = captor.getValue();

        assertEquals(
                EmailDeliveryStatus.NOT_REQUIRED,
                savedNotification.getEmailDeliveryStatus()
        );

        assertNull(savedNotification.getEmailNextAttemptAt());
    }
}
