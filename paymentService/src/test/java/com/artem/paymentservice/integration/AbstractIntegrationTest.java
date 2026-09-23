package com.artem.paymentservice.integration;

import com.artem.paymentservice.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "payment.reconciliation.enabled=false",
        "payment-service.security.username=admin",
        "payment-service.security.password-hash=$2a$10$NbZWJ28OPpT/tYAh6VuZn.20bKwf7AiXLgtCmSYMAVGyLGXFhYm/q"
})
public abstract class AbstractIntegrationTest {

    protected static final WireMockServer wireMockServer =
            new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "fake-payment-provider.base-url",
                wireMockServer::baseUrl
        );
    }
}