package com.artem.currencyrateservice;

import com.artem.currencyrateservice.provider.client.CentralBankClient;
import com.artem.currencyrateservice.provider.dto.CentralBankRate;
import com.artem.currencyrateservice.provider.dto.CentralBankResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CentralBankClientIT {

    @Autowired
    private CentralBankClient client;

    // 🔹 Подменяем URL для Feign на WireMock
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("cbr.url",
                () -> "http://localhost:" + TestcontainersConfiguration.wireMockContainer.getMappedPort(8080));
    }

    // 🔹 Перед каждым тестом сбрасываем WireMock и ставим stub
    @BeforeEach
    void reset() {
        WireMock.reset();
        TestcontainersConfiguration.setupCurrencyRateStub();
    }

    @Test
    void shouldFetchRatesFromWireMockInsteadOfRealCbr() {
        CentralBankResponse response = client.getDailyRates();

        assertThat(response).isNotNull();
        assertThat(response.getValute()).isNotEmpty();

        CentralBankRate usd = response.getValute().stream()
                .filter(v -> "USD".equals(v.getCharCode()))
                .findFirst()
                .orElseThrow();

        assertThat(usd.getValue()).isEqualTo("90,50");
    }
}