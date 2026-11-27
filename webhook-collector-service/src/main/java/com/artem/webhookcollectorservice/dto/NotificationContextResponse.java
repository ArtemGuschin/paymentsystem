package com.artem.webhookcollectorservice.dto;

import java.util.UUID;

public record NotificationContextResponse(
        UUID transactionUid,
        UUID userUid
) {
}