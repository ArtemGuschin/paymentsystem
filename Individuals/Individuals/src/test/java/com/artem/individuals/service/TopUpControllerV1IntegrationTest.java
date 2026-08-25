package com.artem.individuals.service;

import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.response.TokenResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
public class TopUpControllerV1IntegrationTest
        extends TestContainersConfig {

    @DynamicPropertySource
    static void registerPaymentServiceUrl(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "payment-service.url",
                TestContainersConfig.wireMockContainer::getBaseUrl
        );
    }

    private String getAccessToken(
            String email,
            String password
    ) {
        return keycloakIntegrationClient
                .loginUser(email, password)
                .map(TokenResponse::getAccessToken)
                .block();
    }

    private RegistrationRequest createRegistrationRequest(
            String email
    ) {
        RegistrationRequest request =
                new RegistrationRequest();

        request.setEmail(email);
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("user");

        return request;
    }

    private TopUpConfirmRequestDto createTopUpRequest() {

        return TopUpConfirmRequestDto.builder()
                .userUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .comment("test topup")
                .paymentMethodId(1L)
                .currency("USD")
                .paymentFields(
                        Map.of(
                                "cardNumber",
                                "4111111111111111",
                                "cardHolder",
                                "Artem Test",
                                "cvv",
                                "123"
                        )
                )
                .build();
    }

    @Test
    void testTopUpConfirm_Success() {

        String email =
                "topup-success-" + UUID.randomUUID() + "@test.com";

        RegistrationRequest registrationRequest =
                createRegistrationRequest(email);

        keycloakIntegrationClient
                .registerUser(registrationRequest)
                .block();

        String accessToken =
                getAccessToken(
                        email,
                        "password123"
                );

        UUID transactionUid =
                UUID.randomUUID();

        /*
         * 1. Transaction Service
         *
         * Только создаёт PENDING.
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/topup/confirm")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(202)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "transactionUid": "%s",
                                                  "status": "PENDING"
                                                }
                                                """.formatted(transactionUid))
                        )
        );

        /*
         * 2. Payment Service
         *
         * Провайдер успешно обработал платёж.
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/api/v1/payments")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "providerTransactionId": "test-provider-123",
                                                  "status": "SUCCESS"
                                                }
                                                """)
                        )
        );

        /*
         * 3. Transaction Service
         *
         * После SUCCESS зачисляем деньги.
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlPathMatching(
                                        "/topup/.*/complete"
                                )
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "transactionUid": "%s",
                                                  "status": "COMPLETED",
                                                  "providerTransactionId": "test-provider-123"
                                                }
                                                """.formatted(transactionUid))
                        )
        );

        TopUpConfirmRequestDto request =
                createTopUpRequest();

        webTestClient
                .post()
                .uri("/api/v1/topup/confirm")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.transactionUuid")
                .isEqualTo(transactionUid.toString())
                .jsonPath("$.transactionStatus")
                .isEqualTo("COMPLETED")
                .jsonPath("$.providerTransactionId")
                .isEqualTo("test-provider-123")
                .jsonPath("$.paymentStatus")
                .isEqualTo("SUCCESS");

        /*
         * Проверяем весь межсервисный flow.
         */
        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlEqualTo("/topup/confirm")
                )
        );

        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlEqualTo("/api/v1/payments")
                )
        );

        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlPathMatching(
                                "/topup/.*/complete"
                        )
                )
        );
    }

    @Test
    void testTopUpConfirm_Failed() {

        String email =
                "topup-failed-" + UUID.randomUUID() + "@test.com";

        RegistrationRequest registrationRequest =
                createRegistrationRequest(email);

        keycloakIntegrationClient
                .registerUser(registrationRequest)
                .block();

        String accessToken =
                getAccessToken(
                        email,
                        "password123"
                );

        UUID transactionUid =
                UUID.randomUUID();

        /*
         * Transaction Service → PENDING
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/topup/confirm")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(202)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "transactionUid": "%s",
                                                  "status": "PENDING"
                                                }
                                                """.formatted(transactionUid))
                        )
        );

        /*
         * Payment Service → FAILED
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/api/v1/payments")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "providerTransactionId": "test-provider-failed",
                                                  "status": "FAILED"
                                                }
                                                """)
                        )
        );

        /*
         * Transaction Service → FAILED
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlPathMatching(
                                        "/topup/.*/fail"
                                )
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "transactionUid": "%s",
                                                  "status": "FAILED"
                                                }
                                                """.formatted(transactionUid))
                        )
        );

        TopUpConfirmRequestDto request =
                createTopUpRequest();

        webTestClient
                .post()
                .uri("/api/v1/topup/confirm")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.transactionUuid")
                .isEqualTo(transactionUid.toString())
                .jsonPath("$.transactionStatus")
                .isEqualTo("FAILED")
                .jsonPath("$.paymentStatus")
                .isEqualTo("FAILED");

        /*
         * При FAILED completeTopUp вызываться не должен.
         */
        WireMock.verify(
                0,
                WireMock.postRequestedFor(
                        WireMock.urlPathMatching(
                                "/topup/.*/complete"
                        )
                )
        );

        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlPathMatching(
                                "/topup/.*/fail"
                        )
                )
        );

        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlEqualTo("/api/v1/payments")
                )
        );
    }

    @Test
    void testTopUpConfirm_PaymentServiceUnavailable() {

        String email =
                "topup-unavailable-" + UUID.randomUUID() + "@test.com";

        RegistrationRequest registrationRequest =
                createRegistrationRequest(email);

        keycloakIntegrationClient
                .registerUser(registrationRequest)
                .block();

        String accessToken =
                getAccessToken(
                        email,
                        "password123"
                );

        UUID transactionUid =
                UUID.randomUUID();

        /*
         * Transaction Service → PENDING
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/topup/confirm")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(202)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "transactionUid": "%s",
                                                  "status": "PENDING"
                                                }
                                                """.formatted(transactionUid))
                        )
        );

        /*
         * Payment Service → 500
         */
        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/api/v1/payments")
                        )
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(500)
                        )
        );

        TopUpConfirmRequestDto request =
                createTopUpRequest();

        webTestClient
                .post()
                .uri("/api/v1/topup/confirm")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is5xxServerError();

        /*
         * Payment Service действительно был вызван.
         */
        WireMock.verify(
                1,
                WireMock.postRequestedFor(
                        WireMock.urlEqualTo("/api/v1/payments")
                )
        );

        /*
         * complete/fail не должны вызываться,
         * потому что мы не получили нормальный
         * бизнес-ответ от Payment Service.
         */
        WireMock.verify(
                0,
                WireMock.postRequestedFor(
                        WireMock.urlPathMatching(
                                "/topup/.*/complete"
                        )
                )
        );

        WireMock.verify(
                0,
                WireMock.postRequestedFor(
                        WireMock.urlPathMatching(
                                "/topup/.*/fail"
                        )
                )
        );
    }
}