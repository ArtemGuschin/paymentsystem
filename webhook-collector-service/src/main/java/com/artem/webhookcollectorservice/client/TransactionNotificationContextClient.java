package com.artem.webhookcollectorservice.client;

import com.artem.webhookcollectorservice.dto.NotificationContextResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class TransactionNotificationContextClient {

    private final RestClient restClient;

    public TransactionNotificationContextClient(
            RestClient.Builder restClientBuilder,
            @Value("${transaction-service.url:http://localhost:8585}")
            String transactionServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(transactionServiceUrl)
                .build();
    }

    public NotificationContextResponse getNotificationContext(
            UUID transactionUid
    ) {
        return restClient
                .get()
                .uri(
                        "/internal/transactions/{transactionUid}/notification-context",
                        transactionUid
                )
                .retrieve()
                .body(NotificationContextResponse.class);
    }
}