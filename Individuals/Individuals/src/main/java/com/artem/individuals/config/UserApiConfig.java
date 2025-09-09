package com.artem.individuals.config;

import com.artem.ApiClient;
import com.artem.api.UsersApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApiConfig {

    @Value("${person-service.base-url:http://localhost:8091}")
    private String personServiceBaseUrl;

    @Bean
    public ApiClient apiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(personServiceBaseUrl);
        return apiClient;
    }

    @Bean
    public UsersApi usersApi(ApiClient apiClient) {
        return new UsersApi(apiClient);
    }
}