package com.artem.individuals.service;



import com.artem.individuals.dto.request.RegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerV1IntegrationTest2 {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // Перед каждым тестом настраиваем успешный сценарий по умолчанию
        TestContainersConfig2.setupSuccessScenario();
    }

    private RegistrationRequest createValidRegistrationRequest(String email) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(email);
        request.setPassword("qwerty");
        request.setConfirmPassword("qwerty");
        request.setFirstName("Eugen");
        request.setLastName("Suleymanow");
        request.setRole("admin");
        return request;
    }

    @Test
    void testRegistration_Success() {
        RegistrationRequest request = createValidRegistrationRequest("success@example.com");

        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.refreshToken").exists();

        // Проверяем, что был вызван запрос к сервису Person
        verify(postRequestedFor(urlPathEqualTo("/api/v1/users")));

        // Проверяем, что был вызван запрос к Keycloak
        verify(postRequestedFor(urlPathEqualTo("/auth/admin/realms/test-realm/users")));
    }

    @Test
    void testRegistration_PersonServiceError() {
        // Настраиваем сценарий с ошибкой в сервисе Person
        TestContainersConfig.setupPersonServiceErrorScenario();

        RegistrationRequest request = createValidRegistrationRequest("fail@example.com");

        // Отправляем запрос на регистрацию
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем, что был выполнен запрос к Person Service
        verify(postRequestedFor(urlPathEqualTo("/api/v1/users")));

        // Проверяем, что был выполнен компенсирующий запрос на удаление пользователя
        verify(deleteRequestedFor(urlPathMatching("/api/v1/users/compensate/.*")));

        // Проверяем, что запрос к Keycloak НЕ был выполнен
        verify(0, postRequestedFor(urlPathEqualTo("/auth/admin/realms/test-realm/users")));
    }

    @Test
    void testRegistration_KeycloakError() {
        // Настраиваем сценарий с ошибкой в Keycloak
        TestContainersConfig.setupKeycloakErrorScenario();

        RegistrationRequest request = createValidRegistrationRequest("keycloak-fail@example.com");

        // Отправляем запрос на регистрацию
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем, что запрос к Person Service был выполнен
        verify(postRequestedFor(urlPathEqualTo("/api/v1/users")));

        // Проверяем, что запрос к Keycloak был выполнен
        verify(postRequestedFor(urlPathEqualTo("/auth/admin/realms/test-realm/users")));

        // Проверяем, что был выполнен компенсирующий запрос на удаление пользователя
        verify(deleteRequestedFor(urlPathMatching("/api/v1/users/compensate/.*")));
    }
}