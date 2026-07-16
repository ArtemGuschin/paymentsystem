package com.artem.paymentservice.config;

import com.artem.fakepaymentprovider.client.ApiClient;
import com.artem.fakepaymentprovider.client.api.PayoutsApi;
import com.artem.fakepaymentprovider.client.api.TransactionsApi;
import com.artem.fakepaymentprovider.client.api.WebhooksApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakePaymentProviderClientConfig {

    @Bean
    public ApiClient apiClient() {

        ApiClient apiClient = new ApiClient();

        apiClient.setBasePath("http://localhost:8888");

        apiClient.addDefaultHeader(
                "X-Service-Token",
                "payment-service-secret"
        );

        return apiClient;
    }

    @Bean
    public TransactionsApi transactionsApi(ApiClient apiClient) {
        return new TransactionsApi(apiClient);
    }

    @Bean
    public PayoutsApi payoutsApi(ApiClient apiClient) {
        return new PayoutsApi(apiClient);
    }

    @Bean
    public WebhooksApi webhooksApi(ApiClient apiClient) {
        return new WebhooksApi(apiClient);
    }
}