package com.artem.notificationservice.controller;

import com.artem.common.notification.dto.NotificationResponse;
import com.artem.common.notification.dto.NotificationStatus;
import com.artem.common.notification.dto.NotificationStatusUpdateRequest;
import com.artem.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationControllerV1 {

    private final NotificationService notificationService;

    @GetMapping("/{userUid}")
    public List<NotificationResponse> getNotifications(
            @PathVariable String userUid
    ) {
        return notificationService.getNotificationsByUserUid(userUid);
    }

    @PatchMapping("/{id}/status")
    public NotificationResponse updateStatus(
            @PathVariable Long id,
            @RequestBody NotificationStatusUpdateRequest request
    ) {
        if (request == null || request.status() != NotificationStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only transition to COMPLETED is allowed"
            );
        }

        return notificationService.completeNotification(id);
    }
}