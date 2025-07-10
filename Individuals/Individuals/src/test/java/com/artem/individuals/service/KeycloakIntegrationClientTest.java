package com.artem.individuals.service;

import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class KeycloakIntegrationClientTest {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:24.0.2";
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    @Autowired
    private KeycloakIntegrationClient keycloakIntegrationClient;

    @Container
    private static final GenericContainer<?> keycloakContainer = new GenericContainer<>(
            DockerImageName.parse(KEYCLOAK_IMAGE))
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withCommand("start-dev")
            .waitingFor(Wait.forHttp("/admin").forStatusCode(200));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String host = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);

        registry.add("keycloak.server-url", () -> host);
        registry.add("keycloak.realm", () -> REALM_NAME);
        registry.add("keycloak.client-id", () -> CLIENT_ID);
        registry.add("keycloak.client-secret", () -> CLIENT_SECRET);
    }

    @BeforeAll
    static void setupKeycloak() {
        // Настройка Keycloak через API
        String serverUrl = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);

        // Создание Keycloak Admin Client
        org.keycloak.admin.client.Keycloak keycloakAdmin = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .username(ADMIN_USER)
                .password(ADMIN_PASSWORD)
                .clientId("admin-cli")
                .build();

        // Создание realm
        org.keycloak.representations.idm.RealmRepresentation realm = new org.keycloak.representations.idm.RealmRepresentation();
        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);
        keycloakAdmin.realms().create(realm);

        // Создание client
        org.keycloak.representations.idm.ClientRepresentation client = new org.keycloak.representations.idm.ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setDirectAccessGrantsEnabled(true);
        client.setPublicClient(false);
        keycloakAdmin.realm(REALM_NAME).clients().create(client);

        // Создание ролей
        for (String roleName : List.of("user", "admin")) {
            org.keycloak.representations.idm.RoleRepresentation role = new org.keycloak.representations.idm.RoleRepresentation();
            role.setName(roleName);
            keycloakAdmin.realm(REALM_NAME).roles().create(role);
        }
    }

    @AfterAll
    static void tearDown() {
        if (keycloakContainer.isRunning()) {
            keycloakContainer.stop();
        }
    }

    private RegistrationRequest createRegistrationRequest(
            String email, String password, String firstName, String lastName, String role) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setRole(role);
        return request;
    }

    @Test
    @DisplayName("Регистрация пользователя: успешное создание")
    void registerUserSuccess() {
        RegistrationRequest request = createRegistrationRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "user"
        );

        StepVerifier.create(keycloakIntegrationClient.registerUser(request))
                .assertNext(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                    assertTrue(response.getAccessToken().length() > 50);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Авторизация пользователя: успешный вход")
    void loginUserSuccess() {
        // Сначала создаем пользователя
        RegistrationRequest regRequest = createRegistrationRequest(
                "login@test.com",
                "password123",
                "Alice",
                "Smith",
                "user"
        );
        keycloakIntegrationClient.registerUser(regRequest).block();

        StepVerifier.create(keycloakIntegrationClient.loginUser("login@test.com", "password123"))
                .assertNext(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Обновление токена: успешное выполнение")
    void refreshTokenSuccess() {
        // Создаем пользователя и логинимся
        RegistrationRequest regRequest = createRegistrationRequest(
                "refresh@test.com",
                "password123",
                "Bob",
                "Johnson",
                "user"
        );
        TokenResponse loginResponse = keycloakIntegrationClient.registerUser(regRequest).block();

        StepVerifier.create(keycloakIntegrationClient.refreshToken(loginResponse.getRefreshToken()))
                .assertNext(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                    assertNotEquals(loginResponse.getAccessToken(), response.getAccessToken());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Получение информации о пользователе: успешный запрос")
    void getUserByIdSuccess() {
        // Создаем пользователя
        RegistrationRequest regRequest = createRegistrationRequest(
                "userinfo@test.com",
                "password123",
                "Emma",
                "Davis",
                "admin"
        );
        TokenResponse loginResponse = keycloakIntegrationClient.registerUser(regRequest).block();

        // Извлекаем ID пользователя из токена
        String userId = extractUserIdFromToken(loginResponse.getAccessToken());

        StepVerifier.create(keycloakIntegrationClient.getUserById(userId))
                .assertNext(user -> {
                    assertEquals("userinfo@test.com", user.getEmail());    // Используем getEmail()
                    assertTrue(user.getRoles().contains("admin"));         // Используем getRoles()
                    assertNotNull(user.getCreatedAt());                    // Используем getCreatedAt()
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка существования пользователя: пользователь существует")
    void userExistsSuccess() {
        // Создаем пользователя
        RegistrationRequest regRequest = createRegistrationRequest(
                "exists@test.com",
                "password123",
                "Tom",
                "Wilson",
                "user"
        );
        keycloakIntegrationClient.registerUser(regRequest).block();

        StepVerifier.create(keycloakIntegrationClient.userExists("exists@test.com"))
                .assertNext(exists -> assertTrue(exists))
                .verifyComplete();
    }

    @Test
    @DisplayName("Авторизация пользователя: неверные учетные данные")
    void loginUserInvalidCredentials() {
        StepVerifier.create(keycloakIntegrationClient.loginUser("invalid@test.com", "wrongpassword"))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("Обновление токена: неверный refresh token")
    void refreshTokenInvalid() {
        StepVerifier.create(keycloakIntegrationClient.refreshToken("invalid-refresh-token"))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("Получение информации о пользователе: пользователь не найден")
    void getUserByIdNotFound() {
        StepVerifier.create(keycloakIntegrationClient.getUserById("non-existent-user-id"))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    // Вспомогательный метод для извлечения user ID из токена
    private String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedBytes);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payload);
            return node.get("sub").asText();
        } catch (Exception e) {
            return null;
        }
    }
}
