package com.artem.individuals.service;


import com.artem.api.UsersApi;
import com.artem.individuals.config.UserApiConfig;
import com.github.tomakehurst.wiremock.client.WireMock;
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


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TestContainersConfig {
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

        // Настраиваем WireMock
        WireMock.configureFor("localhost", wireMockContainer.getMappedPort(8080));
        setupPersonServiceStubs();

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

    private static void setupPersonServiceStubs() {
        // Заглушки для сервиса Person - успешные сценарии
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"123e4567-e89b-12d3-a456-426614174000\", \"status\": \"created\"}")));

        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/api/v1/users/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"deleted\"}")));

        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/api/v1/users/compensate/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"compensated\"}")));


        // Сценарий с ошибкой в сервисе Person
//        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
//                .inScenario("PersonErrorScenario")
//                .whenScenarioStateIs("Started")
//                .willReturn(WireMock.aResponse()
//                        .withStatus(500)
//                        .withHeader("Content-Type", "application/json")
//                        .withBody("{\"error\": \"Database error\"}")));
    }

    public static void resetStubs() {
        WireMock.reset();
        setupPersonServiceStubs();
    }

    public static void setupPersonServiceErrorScenario() {
        WireMock.reset();
        setupPersonServiceStubs();
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Database error\"}")));
    }

    public static void setupKeycloakErrorScenario() {
        WireMock.reset();
        setupPersonServiceStubs();
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/auth/admin/realms/test-realm/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Keycloak internal error\"}")));
    }

    public static void setupUserExistsScenario() {
        WireMock.reset();
        setupPersonServiceStubs();
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/auth/admin/realms/test-realm/users"))
                .willReturn(WireMock.aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"User already exists\"}")));
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Keycloak properties
        String keycloakUrl = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);
        registry.add("keycloak.server-url", () -> keycloakUrl);
        registry.add("keycloak.realm", () -> "test-realm");
        registry.add("keycloak.client-id", () -> "test-client");
        registry.add("keycloak.client-secret", () -> "test-secret");


        // Person service properties - указываем на WireMock вместо реального сервиса

        registry.add("person.service.base-url", () -> wireMockContainer.getBaseUrl() );

    }
}


