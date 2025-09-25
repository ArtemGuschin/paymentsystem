package com.artem.individuals.service;



import com.artem.api.UsersApi;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import com.github.tomakehurst.wiremock.client.WireMock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TestContainersConfig2 {
    @Autowired
    private UsersApi usersApi;

    // Keycloak Testcontainer
    public static final GenericContainer<?> keycloakContainer = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:24.0.2"))
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCommand("start-dev")
            .waitingFor(Wait.forHttp("/admin").forStatusCode(200));

    // WireMock Testcontainer для перехвата запросов к сервису Person
    public static final WireMockContainer wireMockContainer = new WireMockContainer(
            DockerImageName.parse("wiremock/wiremock:3.13.0"))
            .withExposedPorts(8080);

    static {
        // Запускаем оба контейнера
        keycloakContainer.start();
        wireMockContainer.start();

        // Настраиваем Keycloak
        setupKeycloak();
    }

    private static void setupKeycloak() {
        // Настройка Keycloak через API
        String serverUrl = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);

        // Создание Keycloak Admin Client
        org.keycloak.admin.client.Keycloak keycloakAdmin = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();

        // Создание realm
        org.keycloak.representations.idm.RealmRepresentation realm = new org.keycloak.representations.idm.RealmRepresentation();
        realm.setRealm("test-realm");
        realm.setEnabled(true);
        keycloakAdmin.realms().create(realm);

        // Создание client
        org.keycloak.representations.idm.ClientRepresentation client = new org.keycloak.representations.idm.ClientRepresentation();
        client.setClientId("test-client");
        client.setSecret("test-secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setPublicClient(false);
        keycloakAdmin.realm("test-realm").clients().create(client);

        // Создание ролей
        for (String roleName : java.util.List.of("user", "admin")) {
            org.keycloak.representations.idm.RoleRepresentation role = new org.keycloak.representations.idm.RoleRepresentation();
            role.setName(roleName);
            keycloakAdmin.realm("test-realm").roles().create(role);
        }
    }

    public static void setupSuccessScenario() {
        WireMock.configureFor("localhost", wireMockContainer.getMappedPort(8080));

        // Очищаем предыдущие заглушки
        WireMock.reset();

        // Заглушки для успешного сценария
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201) // Изменено с 200 на 201 для создания
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "123e4567-e89b-12d3-a456-426614174000", 
                                "email": "test@example.com",
                                "status": "ACTIVE"
                            }
                            """)));

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/auth/admin/realms/test-realm/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"keycloak-user-id\"}")));

        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/api/v1/users/compensate/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"compensated\"}")));
    }

    public static void setupPersonServiceErrorScenario() {
        WireMock.configureFor("localhost", wireMockContainer.getMappedPort(8080));
        WireMock.reset();

        // Ошибка в Person Service
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Database error\"}")));

        // Keycloak работает нормально (но не должен вызываться)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/auth/admin/realms/test-realm/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withBody("{\"id\": \"keycloak-user-id\"}")));

        // Компенсирующий запрос
        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/api/v1/users/compensate/.*"))
                .willReturn(WireMock.aResponse().withStatus(200)));
    }

    public static void setupKeycloakErrorScenario() {
        WireMock.configureFor("localhost", wireMockContainer.getMappedPort(8080));
        WireMock.reset();

        // Person Service работает нормально
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "123e4567-e89b-12d3-a456-426614174000", 
                                "email": "test@example.com",
                                "status": "ACTIVE"
                            }
                            """)));

        // Ошибка в Keycloak
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/auth/admin/realms/test-realm/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Keycloak internal error\"}")));

        // Компенсирующий запрос
        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/api/v1/users/compensate/.*"))
                .willReturn(WireMock.aResponse().withStatus(200)));
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Keycloak properties
        String keycloakUrl = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);
        registry.add("keycloak.auth-server-url", () -> keycloakUrl);
        registry.add("keycloak.realm", () -> "test-realm");
        registry.add("keycloak.resource", () -> "test-client");
        registry.add("keycloak.credentials.secret", () -> "test-secret");

        // Person service properties - указываем на WireMock
        registry.add("person.service.base-url", () -> wireMockContainer.getBaseUrl());
    }
}