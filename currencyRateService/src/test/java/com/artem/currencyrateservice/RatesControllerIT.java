package com.artem.currencyrateservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Sql("/test-data.sql")
class RatesControllerIT  {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnRate() {
        webTestClient.get()
                .uri("/rates?from=EUR&to=USD")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rate").isEqualTo(1.1);
    }

    @Test
    void shouldReturnRateForTimestamp() {
        webTestClient.get()
                .uri("/rates?from=EUR&to=USD&timestamp=2026-03-16T09:56:00Z")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rate").isEqualTo(1.1);
    }

    @Test
    void shouldReturnNotFoundWhenRateMissing() {
        webTestClient.get()
                .uri("/api/v1/rates?from=EUR&to=JPY")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnNotFoundForExpiredRate() {
        webTestClient.get()
                .uri("/api/v1/rates?from=EUR&to=USD&timestamp=2020-01-01T00:00:00Z")
                .exchange()
                .expectStatus().isNotFound();
    }
}