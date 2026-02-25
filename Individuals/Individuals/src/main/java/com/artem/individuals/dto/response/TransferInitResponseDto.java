package com.artem.individuals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferInitResponseDto {
    private Boolean available;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private String currency;
    private String message;
}
