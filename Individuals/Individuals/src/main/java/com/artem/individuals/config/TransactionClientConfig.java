package com.artem.individuals.config;

import com.artem.transaction.client.ApiClient;
import com.artem.transaction.client.api.TopUpApi;
import com.artem.transaction.client.api.TransferApi;
import com.artem.transaction.client.api.WalletsApi;
import com.artem.transaction.client.api.WithdrawalApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionClientConfig {

    @Bean
    public ApiClient transactionApiClient(
            @Value("${transaction.service.url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return apiClient;
    }

    @Bean
    public WalletsApi walletsApi(ApiClient transactionApiClient) {
        return new WalletsApi(transactionApiClient);
    }

    @Bean
    public TopUpApi topUpApi(ApiClient transactionApiClient) {
        return new TopUpApi(transactionApiClient);
    }

    @Bean
    public TransferApi transferApi(ApiClient transactionApiClient) {
        return new TransferApi(transactionApiClient);
    }

    @Bean
    public WithdrawalApi withdrawalApi(ApiClient transactionApiClient) {
        return new WithdrawalApi(transactionApiClient);
    }
}
