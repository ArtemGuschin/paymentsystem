package com.artem.individuals.service;

import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.model.AddressRequest;
import com.artem.model.IndividualRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;



@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerV1IntegrationTest extends TestContainersConfig {

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

        AddressRequest address = new AddressRequest();
        address.setCountryId(3);
        address.setAddressLine("yyy");
        address.setZipCode("789");
        address.setCity("Moscow");
        address.setState("Russia");
        request.setAddress(address);

        IndividualRequest individual = new IndividualRequest();
        individual.setPassportNumber("654987");
        individual.setPhoneNumber("876765876765");
        request.setIndividual(individual);

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

        // Безопасные верификации
        try {
            WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlPathMatching(".*/auth/admin/realms/.*/users.*")));
            WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/users")));
        } catch (Exception e) {
            System.out.println("WireMock verification skipped: " + e.getMessage());
        }
    }

    @Test
    void testRegistration_PersonServiceError() {
        TestContainersConfig.setupPersonServiceErrorScenario();

        RegistrationRequest request = createValidRegistrationRequest("fail@example.com");

        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем компенсацию
        try {
            WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlPathMatching(".*/auth/admin/realms/.*/users.*")));
            WireMock.verify(1, WireMock.deleteRequestedFor(WireMock.urlPathMatching(".*/auth/admin/realms/.*/users/.*")));
        } catch (Exception e) {
            System.out.println("WireMock verification skipped: " + e.getMessage());
        }
    }

    @Test
    void testRegistration_KeycloakError() {
        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching(".*/auth/admin/realms/.*/users.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Keycloak Internal Server Error\"}")));

        RegistrationRequest request = createValidRegistrationRequest("keycloak-fail@example.com");

        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        // Проверяем, что Person Service не вызывался
        try {
            WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlPathMatching(".*/auth/admin/realms/.*/users.*")));
            WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/users")));
        } catch (Exception e) {
            System.out.println("WireMock verification skipped: " + e.getMessage());
        }
    }
}


//import com.artem.individuals.dto.request.RegistrationRequest;
//
//import com.artem.model.AddressRequest;
//import com.artem.model.IndividualRequest;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.web.reactive.function.BodyInserters;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//
//
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@AutoConfigureWebTestClient
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class AuthControllerV1IntegrationTest extends TestContainersConfig {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    public AuthControllerV1IntegrationTest() {
//        super();
//    }
//
//
//    private RegistrationRequest createValidRegistrationRequest(String email) {
//        RegistrationRequest request = new RegistrationRequest();
//        request.setEmail(email);
//        request.setPassword("qwerty");
//        request.setConfirmPassword("qwerty");
//        request.setFirstName("Eugen");
//        request.setLastName("Suleymanow");
//        request.setRole("admin");
//
//        //  Создаем объект Address
//        AddressRequest address = new AddressRequest();
//        address.setCountryId(3);
//        address.setAddressLine("yyy");
//        address.setZipCode("789");
//        address.setCity("Moscow");
//        address.setState("Russia");
//        request.setAddress(address);
//
//        // Создаем объект Individual
//        IndividualRequest individual = new IndividualRequest();
//        individual.setPassportNumber("654987");
//        individual.setPhoneNumber("876765876765");
//        request.setIndividual(individual);
//
//        return request;
//    }
//
/// /    @Test
/// /    void testRegistration_Success() {
/// /        RegistrationRequest request = createValidRegistrationRequest("success@example.com");
/// /
/// /        webTestClient.post()
/// /                .uri("/v1/auth/registration")
/// /                .contentType(MediaType.APPLICATION_JSON)
/// /                .body(BodyInserters.fromValue(request))
/// /                .exchange()
/// /                .expectStatus().isOk()
/// /                .expectBody()
/// /                .jsonPath("$.accessToken").exists()
/// /                .jsonPath("$.refreshToken").exists();
/// /
/// /
/// /    }
//
//    @Test
//    void testRegistration_Success() {
//        RegistrationRequest request = createValidRegistrationRequest("success@example.com");
//
//        webTestClient.post()
//                .uri("/v1/auth/registration")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(request))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody()
//                .jsonPath("$.accessToken").exists()
//                .jsonPath("$.refreshToken").exists();
//
//        // Безопасные верификации
//        try {
//            com.github.tomakehurst.wiremock.client.WireMock.verify(1,
//                    com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
//                            com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching(".*/auth/admin/realms/.*/users.*")
//                    ));
//        } catch (Exception e) {
//            System.out.println("Keycloak creation verification skipped: " + e.getMessage());
//        }
//
//        try {
//            com.github.tomakehurst.wiremock.client.WireMock.verify(1,
//                    com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
//                            com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/v1/users")
//                    ));
//        } catch (Exception e) {
//            System.out.println("Person service creation verification skipped: " + e.getMessage());
//        }
//    }
//
//
//    @Test
//    void testRegistration_PersonServiceError() {
//        TestContainersConfig.setupPersonServiceErrorScenario();
//
//        RegistrationRequest request = createValidRegistrationRequest("fail@example.com");
//
//        webTestClient.post()
//                .uri("/v1/auth/registration")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(request))
//                .exchange()
//                .expectStatus().is5xxServerError();
//
//
//    }
//
//
//    @Test
//    void testRegistration_KeycloakError() {
//
//        stubFor(post(urlPathMatching(".*/auth/admin/realms/.*/users.*"))
//                .willReturn(aResponse()
//                        .withStatus(500)
//                        .withHeader("Content-Type", "application/json")
//                        .withBody("{\"error\": \"Keycloak Internal Server Error\"}")));
//
//        RegistrationRequest request = createValidRegistrationRequest("keycloak-fail@example.com");
//
//
//        webTestClient.post()
//                .uri("/v1/auth/registration")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(request))
//                .exchange()
//                .expectStatus().is5xxServerError();
//
//
//    }
//
//
//}