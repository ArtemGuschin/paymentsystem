package com.artem.individuals.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient paymentWebClient(
            WebClient.Builder builder,
            @Value("${payment-service.url}")
            String paymentServiceUrl
    ) {
        return builder
                .baseUrl(paymentServiceUrl)
                .build();
    }

    @Bean
    public WebClient authWebClient(
            WebClient.Builder builder,
            @Value("${auth-service.url}")
            String authServiceUrl
    ) {
        return builder
                .baseUrl(authServiceUrl)
                .build();
    }
}