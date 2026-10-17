package com.artem.transactionservice.dto;

import java.util.UUID;

public record NotificationContextResponse(
        UUID transactionUid,
        UUID userUid
) {
}