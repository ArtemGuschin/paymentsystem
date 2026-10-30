package com.artem.webhookcollectorservice.client;

import com.artem.webhookcollectorservice.dto.NotificationContextResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

@Component
public class TransactionNotificationContextClient {

    private final RestClient restClient;

    public TransactionNotificationContextClient(
            RestClient.Builder restClientBuilder,

            @Value("${transaction-service.url:http://localhost:8585}")
            String transactionServiceUrl,

            @Value("${transaction-service.connect-timeout:2s}")
            Duration connectTimeout,

            @Value("${transaction-service.read-timeout:5s}")
            Duration readTimeout
    ) {

        // Настраиваем ограничения времени ожидания HTTP.
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        // Создаём HTTP-клиент с заданными таймаутами.
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
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