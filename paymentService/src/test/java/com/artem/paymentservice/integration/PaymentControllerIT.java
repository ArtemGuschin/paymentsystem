package com.artem.paymentservice.integration;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.model.PaymentProvider;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentProviderRepository;
import com.artem.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;

    @Test
    void shouldCreatePayment() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("TEST_PROVIDER")
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
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertEquals(
                1,
                paymentRepository.count()
        );
    }
}