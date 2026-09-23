package com.artem.webhookcollectorservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderWebhookPayload {

    private UUID transactionUid;

    private String status;

    private BigDecimal amount;
}