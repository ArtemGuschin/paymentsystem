package com.artem.webhookcollectorservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderWebhookRequest {

    private String eventType;

    private Long entityId;

    private PaymentProviderWebhookPayload payload;
}