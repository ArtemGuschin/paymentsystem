package com.artem.webhookcollectorservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderWebhookRequest {

    @NotBlank
    private String eventType;

    @NotNull
    private Long entityId;

    @Valid
    @NotNull
    private PaymentProviderWebhookPayload payload;
}