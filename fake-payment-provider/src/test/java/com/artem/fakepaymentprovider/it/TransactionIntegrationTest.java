package com.artem.fakepaymentprovider.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.utility.TestcontainersConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TransactionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(9999);
        wireMockServer.start();
        configureFor("localhost", 9999);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setupMock() {
        wireMockServer.resetAll(); // 🔥 важно чтобы тесты не мешали друг другу

        stubFor(post(urlEqualTo("/webhook-test"))
                .willReturn(aResponse().withStatus(200)));
    }

    // ✅ HAPPY PATH
    @Test
    @Sql("/merchant.sql")
    void should_create_transaction_and_send_webhook() {

        HttpEntity<String> request = buildRequest("tx-test-1");

        restTemplate.withBasicAuth("merchant_001", "test_secret")
                .postForEntity(getUrl(), request, String.class);

        await().atMost(5, SECONDS)
                .untilAsserted(() ->
                        verify(postRequestedFor(urlEqualTo("/webhook-test")))
                );
    }

    // ❌ 401
    @Test
    void should_return_401_when_invalid_credentials() {

        HttpEntity<String> request = buildRequest("tx-test-2");

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("wrong", "wrong")
                .postForEntity(getUrl(), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ❌ 400 validation
    @Test
    @Sql("/merchant.sql")
    void should_return_400_when_invalid_request() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "amount": -100
                }
                """;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("merchant_001", "test_secret")
                .postForEntity(getUrl(), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ❌ duplicate externalId
    @Test
    @Sql("/merchant.sql")
    void should_not_create_duplicate_transaction() {

        HttpEntity<String> request = buildRequest("tx-duplicate");

        restTemplate.withBasicAuth("merchant_001", "test_secret")
                .postForEntity(getUrl(), request, String.class);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("merchant_001", "test_secret")
                .postForEntity(getUrl(), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ❌ webhook failure
    @Test
    @Sql("/merchant.sql")
    void should_call_webhook_even_if_it_fails() {

        // webhook падает
        stubFor(post(urlEqualTo("/webhook-test"))
                .willReturn(aResponse().withStatus(500)));

        HttpEntity<String> request = buildRequest("tx-fail");

        restTemplate.withBasicAuth("merchant_001", "test_secret")
                .postForEntity(getUrl(), request, String.class);

        await().atMost(5, SECONDS)
                .untilAsserted(() ->
                        verify(postRequestedFor(urlEqualTo("/webhook-test")))
                );
    }

    // =========================
    // 🔧 helpers
    // =========================

    private HttpEntity<String> buildRequest(String externalId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "amount": 100,
                  "currency": "USD",
                  "method": "CARD",
                  "externalId": "%s",
                  "notificationUrl": "http://localhost:9999/webhook-test"
                }
                """.formatted(externalId);

        return new HttpEntity<>(body, headers);
    }

    private String getUrl() {
        return "http://localhost:" + port + "/api/v1/transactions";
    }
}