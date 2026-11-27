package com.artem.notificationservice.service;

import com.artem.common.notification.dto.NotificationResponse;
import com.artem.notificationservice.model.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getMessage(),
                entity.getUserUid(),
                entity.getNotificationStatus(),
                entity.getSubject(),
                entity.getCreatedBy()
        );
    }
}