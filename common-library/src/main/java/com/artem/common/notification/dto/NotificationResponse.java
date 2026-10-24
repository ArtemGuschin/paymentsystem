package com.artem.common.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String message,
        String userUid,
        NotificationStatus notificationStatus,
        String subject,
        String createdBy
) {
}