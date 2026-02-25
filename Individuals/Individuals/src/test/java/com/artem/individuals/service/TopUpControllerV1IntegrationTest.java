package com.artem.individuals.service;

import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.response.TokenResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class TopUpControllerV1IntegrationTest extends TestContainersConfig {


    private String getAccessToken(String email, String password) {
        return keycloakIntegrationClient
                .loginUser(email, password)
                .map(TokenResponse::getAccessToken)
                .block();
    }

    @Test
    void testTopUpConfirm_Success() {

        RegistrationRequest regRequest = new RegistrationRequest();
        regRequest.setEmail("topup@test.com");
        regRequest.setPassword("password123");
        regRequest.setFirstName("Test");
        regRequest.setLastName("User");
        regRequest.setRole("user");

        keycloakIntegrationClient.registerUser(regRequest).block();

        String accessToken = getAccessToken("topup@test.com", "password123");

        TopUpConfirmRequestDto request = TopUpConfirmRequestDto.builder()
                .userUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .comment("test topup")
                .build();

        webTestClient.post()
                .uri("/api/v1/topup/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.transactionUuid").isNotEmpty();

        WireMock.verify(1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/topup/confirm")));
    }


    @Test
    void testTopUpConfirm_Error() {

        RegistrationRequest regRequest = new RegistrationRequest();
        regRequest.setEmail("topup2@test.com");
        regRequest.setPassword("password123");
        regRequest.setFirstName("Test");
        regRequest.setLastName("User");
        regRequest.setRole("user");

        keycloakIntegrationClient.registerUser(regRequest).block();

        String token = getAccessToken("topup2@test.com", "password123");

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/topup/confirm"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        TopUpConfirmRequestDto request = TopUpConfirmRequestDto.builder()
                .userUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .comment("test topup")
                .build();

        webTestClient.post()
                .uri("/api/v1/topup/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }


}
