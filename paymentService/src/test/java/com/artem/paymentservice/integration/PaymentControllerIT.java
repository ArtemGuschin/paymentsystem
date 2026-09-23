package com.artem.paymentservice.integration;

import com.artem.paymentservice.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artem.paymentservice.model.Payment;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.model.PaymentProvider;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentProviderRepository;
import com.artem.paymentservice.repository.PaymentRepository;
import com.artem.paymentservice.service.PaymentReconciliationService;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.artem.paymentservice.model.PaymentMethodDefinition;
import com.artem.paymentservice.repository.PaymentMethodDefinitionRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;

    @Autowired
    private PaymentMethodDefinitionRepository paymentMethodDefinitionRepository;



    @Autowired
    private PaymentReconciliationService paymentReconciliationService;

    @BeforeEach
    void setUp() {

        paymentRepository.deleteAll();
        paymentMethodDefinitionRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        paymentProviderRepository.deleteAll();

        wireMockServer.resetAll();

        /*
         * Fake Payment Provider:
         *
         * POST /api/v1/transactions
         * -> создаём внешнюю транзакцию с id=123
         */
        wireMockServer.stubFor(
                WireMock.post(
                                urlPathEqualTo("/api/v1/transactions")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                        {
                                          "id": 123,
                                          "status": "PENDING"
                                        }
                                        """)
                        )
        );

        /*
         * Fake Payment Provider:
         *
         * GET /api/v1/transactions/123
         * -> возвращаем SUCCESS
         */
        wireMockServer.stubFor(
                WireMock.get(
                                urlEqualTo("/api/v1/transactions/123")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                        {
                                          "id": 123,
                                          "status": "SUCCESS"
                                        }
                                        """)
                        )
        );
    }

    @Test
    void shouldCreatePayment() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(
                                        UUID.randomUUID().toString()
                                )
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String request = """
                {
                  "internalTransactionUid":"11111111-1111-1111-1111-111111111111",
                  "methodId": %d,
                  "amount": 100.50,
                  "currency": "EUR",
                  "userFields": {
                    "cardNumber":"4111111111111111"
                  }
                }
                """.formatted(method.getId());

        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(
                        jsonPath("$.providerTransactionId")
                                .value("123")
                );

        assertEquals(
                1,
                paymentRepository.count()
        );
    }
    @Test
    void shouldBeIdempotentForRepeatedInternalTransactionUid() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(UUID.randomUUID().toString())
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String request = """
            {
              "internalTransactionUid":"22222222-2222-2222-2222-222222222222",
              "methodId": %d,
              "amount": 100.50,
              "currency": "EUR",
              "userFields": {
                "cardNumber":"4111111111111111"
              }
            }
            """.formatted(method.getId());

        // Первый запрос
        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.providerTransactionId").value("123"));

        // Второй запрос с тем же internalTransactionUid
        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.providerTransactionId").value("123"));

        // В БД должен остаться только один Payment
        assertEquals(
                1,
                paymentRepository.count()
        );

        // Внешний провайдер должен быть вызван только один раз
        wireMockServer.verify(
                1,
                com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                        urlPathEqualTo("/api/v1/transactions")
                )
        );
    }
    @Test
    void shouldBeIdempotentForConcurrentRequests() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(UUID.randomUUID().toString())
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String request = """
            {
              "internalTransactionUid":"33333333-3333-3333-3333-333333333333",
              "methodId": %d,
              "amount": 100.50,
              "currency": "EUR",
              "userFields": {
                "cardNumber":"4111111111111111"
              }
            }
            """.formatted(method.getId());

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        try {

            Callable<String> requestTask = () ->
                    mockMvc.perform(
                                    post("/api/v1/payments")
                                            .with(httpBasic("admin", "admin"))
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(request)
                            )
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            Future<String> firstRequest =
                    executorService.submit(requestTask);

            Future<String> secondRequest =
                    executorService.submit(requestTask);

            String firstResponse =
                    firstRequest.get();

            String secondResponse =
                    secondRequest.get();

            assertTrue(
                    firstResponse.contains("\"status\":\"SUCCESS\"")
                            || firstResponse.contains("\"status\": \"SUCCESS\"")
            );

            assertTrue(
                    secondResponse.contains("\"status\":\"SUCCESS\"")
                            || secondResponse.contains("\"status\": \"SUCCESS\"")
            );

            assertEquals(
                    1,
                    paymentRepository.count()
            );

            wireMockServer.verify(
                    1,
                    com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                            urlPathEqualTo("/api/v1/transactions")
                    )
            );

        } finally {
            executorService.shutdown();
        }
    }
    @Test
    void shouldKeepPaymentPendingWhenProviderStatusIsUnknown()
            throws Exception {

        wireMockServer.resetAll();

        wireMockServer.stubFor(
                WireMock.post(
                                urlPathEqualTo("/api/v1/transactions")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 456,
                                      "status": "PENDING"
                                    }
                                    """)
                        )
        );

        wireMockServer.stubFor(
                WireMock.get(
                                urlEqualTo("/api/v1/transactions/456")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 456,
                                      "status": "PENDING"
                                    }
                                    """)
                        )
        );

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(UUID.randomUUID().toString())
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String request = """
            {
              "internalTransactionUid":"44444444-4444-4444-4444-444444444444",
              "methodId": %d,
              "amount": 100.50,
              "currency": "EUR",
              "userFields": {
                "cardNumber":"4111111111111111"
              }
            }
            """.formatted(method.getId());

        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(
                        jsonPath("$.providerTransactionId")
                                .value("456")
                );

        Payment payment =
                paymentRepository
                        .findByInternalTransactionId(
                                "44444444-4444-4444-4444-444444444444"
                        )
                        .orElseThrow();

        assertEquals("PENDING", payment.getStatus());
        assertEquals("456", payment.getExternalTransactionId());
        assertEquals(1, paymentRepository.count());
    }
    @Test
    void shouldKeepPaymentPendingWhenProviderStatusCannotBeRead()
            throws Exception {

        wireMockServer.resetAll();

        /*
         * Провайдер принимает платеж и возвращает providerTransactionId.
         */
        wireMockServer.stubFor(
                WireMock.post(
                                urlPathEqualTo("/api/v1/transactions")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 789,
                                      "status": "PENDING"
                                    }
                                    """)
                        )
        );

        /*
         * При попытке узнать статус провайдер отвечает ошибкой.
         *
         * Это НЕ означает FAILED.
         * Результат операции неизвестен.
         */
        wireMockServer.stubFor(
                WireMock.get(
                                urlEqualTo("/api/v1/transactions/789")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                        )
        );

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(UUID.randomUUID().toString())
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String request = """
            {
              "internalTransactionUid":"55555555-5555-5555-5555-555555555555",
              "methodId": %d,
              "amount": 100.50,
              "currency": "EUR",
              "userFields": {
                "cardNumber":"4111111111111111"
              }
            }
            """.formatted(method.getId());

        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(
                        jsonPath("$.providerTransactionId")
                                .value("789")
                );

        Payment payment =
                paymentRepository
                        .findByInternalTransactionId(
                                "55555555-5555-5555-5555-555555555555"
                        )
                        .orElseThrow();

        assertEquals("PENDING", payment.getStatus());
        assertEquals("789", payment.getExternalTransactionId());
        assertEquals(1, paymentRepository.count());
    }
    @Test
    void shouldReconcilePendingPaymentToSuccess() throws Exception {

        wireMockServer.resetAll();

        /*
         * Первый этап:
         * провайдер принял платеж, но статус пока PENDING.
         */
        wireMockServer.stubFor(
                WireMock.post(
                                urlPathEqualTo("/api/v1/transactions")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 900,
                                      "status": "PENDING"
                                    }
                                    """)
                        )
        );

        wireMockServer.stubFor(
                WireMock.get(
                                urlEqualTo("/api/v1/transactions/900")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 900,
                                      "status": "PENDING"
                                    }
                                    """)
                        )
        );

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(
                                        UUID.randomUUID().toString()
                                )
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );
        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        String internalTransactionId =
                "66666666-6666-6666-6666-666666666666";

        String request = """
            {
              "internalTransactionUid":"66666666-6666-6666-6666-666666666666",
              "methodId": %d,
              "amount": 100.50,
              "currency": "EUR",
              "userFields": {
                "cardNumber":"4111111111111111"
              }
            }
            """.formatted(method.getId());

        /*
         * Создаём платеж.
         *
         * Provider останется PENDING,
         * поэтому Payment тоже должен остаться PENDING.
         */
        mockMvc.perform(
                        post("/api/v1/payments")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(
                        jsonPath("$.providerTransactionId")
                                .value("900")
                );

        Payment pendingPayment =
                paymentRepository
                        .findByInternalTransactionId(
                                internalTransactionId
                        )
                        .orElseThrow();

        assertEquals(
                "PENDING",
                pendingPayment.getStatus()
        );

        assertEquals(
                "900",
                pendingPayment.getExternalTransactionId()
        );

        /*
         * Теперь provider уже завершил операцию.
         *
         * Перенастраиваем WireMock:
         * следующий GET вернёт SUCCESS.
         */
        wireMockServer.stubFor(
                WireMock.get(
                                urlEqualTo("/api/v1/transactions/900")
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "id": 900,
                                      "status": "SUCCESS"
                                    }
                                    """)
                        )
        );

        /*
         * Запускаем reconciliation вручную.
         *
         * Мы НЕ ждём 30 секунд Scheduler.
         */
        paymentReconciliationService.reconcilePendingPayments();

        Payment reconciledPayment =
                paymentRepository
                        .findByInternalTransactionId(
                                internalTransactionId
                        )
                        .orElseThrow();

        assertEquals(
                "SUCCESS",
                reconciledPayment.getStatus()
        );

        assertEquals(
                "900",
                reconciledPayment.getExternalTransactionId()
        );
    }
    @Test
    void shouldReturn400ForNegativeAmount() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("FAKE")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod method =
                paymentMethodRepository.save(
                        PaymentMethod.builder()
                                .provider(provider)
                                .type("CARD")
                                .name("Visa")
                                .active(true)
                                .providerUniqueId(UUID.randomUUID().toString())
                                .providerMethodType("CARD")
                                .profileType("INDIVIDUAL")
                                .build()
                );

        paymentMethodDefinitionRepository.save(
                PaymentMethodDefinition.builder()
                        .paymentMethod(method)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isAllCurrencies(false)
                        .isAllCountries(true)
                        .isPriority(true)
                        .isActive(true)
                        .build()
        );

        PaymentRequest request =
                new PaymentRequest()
                        .internalTransactionUid(UUID.randomUUID())
                        .methodId(method.getId().longValue())
                        .amount(new BigDecimal("-100.00"))
                        .currency("EUR")
                        .userFields(Map.of(
                                "iban",
                                "NL91ABNA0417164300"
                        ));

        mockMvc.perform(post("/api/v1/payments")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }
}