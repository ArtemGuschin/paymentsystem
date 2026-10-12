package com.artem.transactionservice.kafka;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentStatusUpdatedEvent {

    private UUID transactionUid;
    private String status;
}