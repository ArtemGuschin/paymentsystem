package com.artem.currencyrateservice;


import com.artem.currencyrateservice.IntegrationTestBase;
import com.artem.currencyrateservice.entity.RateProvider;
import com.artem.currencyrateservice.repository.RateProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

@AutoConfigureWebTestClient
class RateProvidersControllerIT extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RateProviderRepository rateProviderRepository;


    @BeforeEach
    void setUp() {
        rateProviderRepository.deleteAll();
        rateProviderRepository
                .save(
                        RateProvider.builder()
                                .providerCode("ECB")
                                .providerName("European Central Bank")
                                .description("Main provider")
                                .priority(1)
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .modifiedAt(LocalDateTime.now())
                                .build()
                );

        rateProviderRepository.save(
                RateProvider.builder()
                        .providerCode("NB")
                        .providerName("National Bank")
                        .description("Secondary provider")
                        .priority(2)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .modifiedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Test
    void shouldReturnProviders() {
        webTestClient.get()
                .uri("/rate-providers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].providerCode").isEqualTo("ECB")
                .jsonPath("$[1].providerCode").isEqualTo("NB");
    }

    @Test
    void shouldReturnEmptyListWhenNoProviders() {
        rateProviderRepository.deleteAll();

        webTestClient.get()
                .uri("/rate-providers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
