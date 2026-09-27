package com.artem.webhookcollectorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderWebhookPayload {

    @NotNull
    private UUID transactionUid;

    @NotBlank
    private String status;

    @NotNull
    private BigDecimal amount;
}