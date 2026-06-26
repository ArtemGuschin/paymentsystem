package com.artem.paymentservice.integration;

import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.model.PaymentMethodDefinition;
import com.artem.paymentservice.model.PaymentMethodRequiredField;
import com.artem.paymentservice.model.PaymentProvider;
import com.artem.paymentservice.repository.PaymentMethodDefinitionRepository;
import com.artem.paymentservice.repository.PaymentMethodRepository;
import com.artem.paymentservice.repository.PaymentMethodRequiredFieldRepository;
import com.artem.paymentservice.repository.PaymentProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

class PaymentMethodControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentMethodDefinitionRepository definitionRepository;

    @Autowired
    private PaymentMethodRequiredFieldRepository fieldRepository;

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;

    @BeforeEach
    void cleanDatabase() {

        fieldRepository.deleteAll();
        definitionRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        paymentProviderRepository.deleteAll();
    }

    @Test
    void shouldReturnAvailablePaymentMethods() throws Exception {

        PaymentProvider provider =
                paymentProviderRepository.save(
                        PaymentProvider.builder()
                                .name("TEST_PROVIDER")
                                .description("Test provider")
                                .build()
                );

        PaymentMethod paymentMethod =
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

        PaymentMethodDefinition definition =
                PaymentMethodDefinition.builder()
                        .paymentMethod(paymentMethod)
                        .currencyCode("EUR")
                        .countryAlpha3Code("NLD")
                        .isActive(true)
                        .isAllCurrencies(false)
                        .isAllCountries(false)
                        .isPriority(false)
                        .build();

        definitionRepository.save(definition);

        PaymentMethodRequiredField field =
                PaymentMethodRequiredField.builder()
                        .uid(UUID.randomUUID())
                        .paymentMethod(paymentMethod)
                        .name("cardNumber")
                        .dataType("STRING")
                        .isActive(true)
                        .build();

        fieldRepository.save(field);

        mockMvc.perform(
                        get("/api/v1/payment-methods/EUR/NLD")
                                .with(httpBasic("admin", "admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Visa"))
                .andExpect(jsonPath("$[0].providerMethodType").value("CARD"))
                .andExpect(jsonPath("$[0].requiredFields[0].name").value("cardNumber"));
    }

    @Test
    void shouldReturn401WhenUnauthorized() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payment-methods/EUR/NLD")
                )
                .andExpect(status().isUnauthorized());
    }
}