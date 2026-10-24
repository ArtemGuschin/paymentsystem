package com.artem.webhookcollectorservice.dto;

public record NotificationOutboxPayload(
        String eventId,
        String notificationType,
        String message,
        String transactionUid,
        String email,
        String subject,
        String createdBy,
        long createdAtEpochMillis
) {
}