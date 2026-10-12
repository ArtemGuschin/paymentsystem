package com.artem.webhookcollectorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdatedEvent {

    private UUID transactionUid;

    private String status;
}