package com.artem.individuals.payment;

import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentControllerV1IntegrationTest
        extends PaymentIntegrationTestBase {

    @Test
    void processPayment_success() {

        String email =
                "payment-" + UUID.randomUUID() + "@test.com";

        RegistrationRequest registrationRequest =
                new RegistrationRequest();

        registrationRequest.setEmail(email);
        registrationRequest.setPassword("password123");
        registrationRequest.setConfirmPassword("password123");
        registrationRequest.setFirstName("Artem");
        registrationRequest.setLastName("Payment");
        registrationRequest.setRole("user");

        keycloakIntegrationClient
                .registerUser(registrationRequest)
                .block();

        String accessToken =
                getAccessToken(
                        email,
                        "password123"
                );

        assertNotNull(accessToken);

        WireMock.stubFor(
                post(urlEqualTo("/api/v1/payments"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "providerTransactionId": "test-provider-123"
                                                }
                                                """)
                        )
        );

        PaymentRequestDto request =
                new PaymentRequestDto();

        request.setInternalTransactionUid(
                UUID.randomUUID()
        );

        request.setMethodId(1L);
        request.setAmount(1500.00);
        request.setCurrency("USD");

        request.setUserFields(
                Map.of(
                        "cardNumber",
                        "4111111111111111",
                        "cardHolder",
                        "Artem Test",
                        "cvv",
                        "123"
                )
        );

        webTestClient
                .post()
                .uri("/api/v1/payments")
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.providerTransactionId")
                .isEqualTo("test-provider-123");

        WireMock.verify(
                1,
                postRequestedFor(
                        urlEqualTo("/api/v1/payments")
                )
        );
    }

    @Test
    void processPayment_paymentServiceUnavailable() {

        WireMock.stubFor(
                post(urlEqualTo("/api/v1/payments"))
                        .willReturn(
                                aResponse()
                                        .withStatus(502)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "message": "Payment provider is temporarily unavailable"
                                                }
                                                """)
                        )
        );

        PaymentRequestDto request =
                new PaymentRequestDto();

        request.setInternalTransactionUid(
                UUID.randomUUID()
        );

        request.setMethodId(1L);
        request.setAmount(1500.00);
        request.setCurrency("USD");

        request.setUserFields(
                Map.of(
                        "cardNumber",
                        "4111111111111111",
                        "cardHolder",
                        "Artem Test",
                        "cvv",
                        "123"
                )
        );

        webTestClient
                .post()
                .uri("/api/v1/payments")
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(502)
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(502)
                .jsonPath("$.error")
                .isEqualTo("Payment service request failed");

        WireMock.verify(
                1,
                postRequestedFor(
                        urlEqualTo("/api/v1/payments")
                )
        );
    }
}