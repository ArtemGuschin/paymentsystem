package com.artem.individuals.payment;

import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.service.TestContainersConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
@Import(PaymentIntegrationTestConfig.class)
public abstract class PaymentIntegrationTestBase
        extends TestContainersConfig {

    protected static final WireMockContainer paymentWireMockContainer =
            new WireMockContainer(
                    "wiremock/wiremock:3.13.0"
            );

    static {
        paymentWireMockContainer.start();

        configureFor(
                paymentWireMockContainer.getHost(),
                paymentWireMockContainer.getMappedPort(8080)
        );
    }

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected KeycloakIntegrationClient keycloakIntegrationClient;

    @DynamicPropertySource
    static void registerPaymentTestProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "payment-service.url",
                paymentWireMockContainer::getBaseUrl
        );
    }

    @BeforeEach
    void resetPaymentWireMock() {

        configureFor(
                paymentWireMockContainer.getHost(),
                paymentWireMockContainer.getMappedPort(8080)
        );

        reset();
    }

    @AfterAll
    static void stopPaymentWireMock() {

        if (paymentWireMockContainer.isRunning()) {
            paymentWireMockContainer.stop();
        }
    }

    protected String getAccessToken(
            String email,
            String password
    ) {

        TokenResponse response =
                keycloakIntegrationClient
                        .loginUser(email, password)
                        .block();

        if (response == null || response.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Failed to obtain access token"
            );
        }

        return response.getAccessToken();
    }
}