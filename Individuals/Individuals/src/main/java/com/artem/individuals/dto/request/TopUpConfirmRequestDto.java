package com.artem.individuals.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpConfirmRequestDto {

    @NotNull
    private UUID userUid;

    @NotNull
    private UUID walletUid;

    @NotNull
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;

    private String comment;

    @NotNull
    private Long paymentMethodId;

    @NotBlank
    private String currency;

    @NotBlank
    @Size(min = 3, max = 3)
    private String countryCode;

    @NotNull
    private Map<String, String> paymentFields;
}