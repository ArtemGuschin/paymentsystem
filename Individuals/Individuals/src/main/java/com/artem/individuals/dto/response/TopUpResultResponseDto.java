package com.artem.individuals.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class TopUpResultResponseDto {

    private UUID transactionUuid;

    private String transactionStatus;

    private String providerTransactionId;

    private String paymentStatus;
}