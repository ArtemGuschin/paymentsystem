package com.artem.currencyrateservice;

import com.artem.currencyrateservice.entity.Currency;
import com.artem.currencyrateservice.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CurrenciesControllerIT extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        currencyRepository.deleteAll();

        currencyRepository.save(Currency.builder()
                .code("EUR")
                .isoCode(978)
                .description("Euro")
                .active(true)
                .symbol("€")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build());

        currencyRepository.save(Currency.builder()
                .code("USD")
                .isoCode(840)
                .description("US Dollar")
                .active(true)
                .symbol("$")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void shouldReturnCurrencies() {
        webTestClient.get()
                .uri("/currencies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].code").exists()
                .jsonPath("$[1].code").exists();
    }

    @Test
    void shouldReturnEmptyListWhenNoCurrencies() {
        currencyRepository.deleteAll();

        webTestClient.get()
                .uri("/currencies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[]");
    }
}
