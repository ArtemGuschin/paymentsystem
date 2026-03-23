package com.artem.currencyrateservice;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // 🔹 WireMock через Testcontainers
    public static final WireMockContainer wireMockContainer =
            new WireMockContainer(DockerImageName.parse("wiremock/wiremock:3.13.0"))
                    .withExposedPorts(8080);

    static {
        // старт контейнера
        wireMockContainer.start();
        WireMock.configureFor("localhost", wireMockContainer.getMappedPort(8080));

        setupCurrencyRateStub();
    }

    // 🔹 Публичный метод для переиспользования в тестах
    public static void setupCurrencyRateStub() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/scripts/XML_daily.asp"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <ValCurs Date="23.03.2026" name="Foreign Currency Market">
                                    <Valute ID="R01235">
                                        <CharCode>USD</CharCode>
                                        <Nominal>1</Nominal>
                                        <Value>90,50</Value>
                                    </Valute>
                                    <Valute ID="R01239">
                                        <CharCode>EUR</CharCode>
                                        <Nominal>1</Nominal>
                                        <Value>98,30</Value>
                                    </Valute>
                                </ValCurs>
                                """)));
    }
}