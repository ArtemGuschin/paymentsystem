package com.artem.individuals.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpInitRequestDto {
    private UUID userUid;
    private UUID walletUid;
    private BigDecimal amount;
}
