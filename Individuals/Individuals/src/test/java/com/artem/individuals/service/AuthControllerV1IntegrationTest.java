package com.artem.individuals.service;


import com.artem.individuals.dto.request.RegistrationRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerV1IntegrationTest  extends TestContainersConfig2 {

    @Autowired
    private WebTestClient webTestClient;

    public AuthControllerV1IntegrationTest() {
        super();
    }


    private RegistrationRequest createValidRegistrationRequest(String email) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(email);
        request.setPassword("qwerty");
        request.setConfirmPassword("qwerty");
        request.setFirstName("Eugen");
        request.setLastName("Suleymanow");
        request.setRole("admin");

        // Создаем объект Address
//        Address address = new Address();
//        address.setCountryId("2");
//        address.setAddressLine("yyy");
//        address.setZipCode("789");
//        address.setCity("Moscow");
//        address.setState("Russia");
//        request.setAddress(address);
//
//        // Создаем объект Individual
//        Individual individual = new Individual();
//        individual.setPassportNumber("654987");
//        individual.setPhoneNumber("876765876765");
//        individual.setStatus("ACTIVE");
//        request.setIndividual(individual);

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
    }

    @Test
    void testRegistration_PersonServiceError() {
        // Настраиваем сценарий с ошибкой в сервисе Person
        TestContainersConfig2.setupPersonServiceErrorScenario();

        RegistrationRequest request = createValidRegistrationRequest("fail@example.com");

        // Отправляем запрос на регистрацию
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем, что был выполнен компенсирующий запрос на удаление пользователя
        // (предполагая, что ваше приложение вызывает компенсирующий эндпоинт)
        verify(deleteRequestedFor(urlPathMatching("/api/v1/users/compensate/.*")));

        // Проверяем, что запрос к сервису Person был выполнен
        verify(postRequestedFor(urlPathEqualTo("/api/v1/users")));
    }

    @Test
    void testRegistration_KeycloakError() {
        // Настраиваем сценарий с ошибкой в Keycloak
        TestContainersConfig2.setupKeycloakErrorScenario();

        RegistrationRequest request = createValidRegistrationRequest("keycloak-fail@example.com");

        // Отправляем запрос на регистрацию
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем, что запрос к Keycloak был выполнен
        verify(postRequestedFor(urlPathEqualTo("/auth/admin/realms/test-realm/users")));

        // Проверяем, что запрос к сервису Person НЕ был выполнен (компенсация не требуется)
        verify(0, postRequestedFor(urlPathEqualTo("/api/v1/users")));
    }


}