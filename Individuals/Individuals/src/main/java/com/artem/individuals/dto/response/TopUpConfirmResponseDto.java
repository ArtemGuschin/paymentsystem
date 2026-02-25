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
public class TopUpConfirmResponseDto {
    private UUID transactionUuid;
    private String status;
}
