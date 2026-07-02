package com.artem.individuals.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotNull
    private UUID internalTransactionUid;

    @NotNull
    private Long methodId;

    @NotNull
    private Double amount;

    @NotBlank
    private String currency;

    @NotNull
    private Map<String, String> userFields;

}