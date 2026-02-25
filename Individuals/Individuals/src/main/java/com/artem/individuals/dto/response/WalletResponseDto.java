package com.artem.individuals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Data
public class WalletResponseDto {
    private UUID walletUid;
    private String name;
    private UUID userUid;
    private UUID walletTypeUid;
    private String walletTypeName;
    private String walletTypeCurrencyCode;
    private BigDecimal balance;
    private String status;
    private OffsetDateTime createdAt;
}