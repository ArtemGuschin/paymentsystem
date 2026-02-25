package com.artem.individuals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalConfirmResponseDto {
    private UUID transactionUid;
    private String status;
}
