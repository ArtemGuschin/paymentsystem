package com.artem.webhookcollectorservice.integration;

import com.artem.webhookcollectorservice.TestcontainersConfiguration;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.entity.PaymentProviderCallbackEntity;
import com.artem.webhookcollectorservice.outbox.OutboxStatus;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import com.artem.webhookcollectorservice.repository.PaymentProviderCallbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "webhook.security.token=test-secret",
        "spring.task.scheduling.enabled=false"
})
class PaymentProviderWebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentProviderCallbackRepository callbackRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        callbackRepository.deleteAll();
    }

    @Test
    void shouldSaveCallbackAndOutboxEventWhenTokenIsValid() throws Exception {

        UUID transactionUid =
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        String body = """
                {
                  "eventType": "TRANSACTION_SUCCESS",
                  "entityId": 57,
                  "payload": {
                    "transactionUid": "%s",
                    "status": "SUCCESS",
                    "amount": 100.00
                  }
                }
                """.formatted(transactionUid);

        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "test-secret")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isOk());

        List<PaymentProviderCallbackEntity> callbacks =
                callbackRepository.findAll();

        List<OutboxEventEntity> outboxEvents =
                outboxRepository.findAll();

        assertThat(callbacks).hasSize(1);
        assertThat(outboxEvents).hasSize(1);

        PaymentProviderCallbackEntity callback = callbacks.getFirst();

        assertThat(callback.getProviderTransactionId()).isEqualTo(57L);
        assertThat(callback.getType()).isEqualTo("TRANSACTION_SUCCESS");
        assertThat(callback.getProvider()).isEqualTo("FAKE_PAYMENT_PROVIDER");

        OutboxEventEntity outboxEvent = outboxEvents.getFirst();

        assertThat(outboxEvent.getAggregateId()).isEqualTo(transactionUid);
        assertThat(outboxEvent.getEventType())
                .isEqualTo("payment.status.updated");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.NEW);

        assertThat(outboxEvent.getPayload())
                .contains(transactionUid.toString())
                .contains("SUCCESS");
    }

    @Test
    void shouldRejectWebhookAndSaveNothingWhenTokenIsInvalid() throws Exception {

        String body = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 58,
              "payload": {
                "transactionUid": "650e8400-e29b-41d4-a716-446655440000",
                "status": "SUCCESS",
                "amount": 500.00
              }
            }
            """;

        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "wrong-secret")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isUnauthorized());

        assertThat(callbackRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
    }
    @Test
    void shouldNotCreateDuplicatesWhenSameWebhookReceivedTwice() throws Exception {

        UUID transactionUid =
                UUID.fromString("750e8400-e29b-41d4-a716-446655440000");

        String body = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 59,
              "payload": {
                "transactionUid": "%s",
                "status": "SUCCESS",
                "amount": 700.00
              }
            }
            """.formatted(transactionUid);

        // Первый webhook
        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "test-secret")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isOk());

        // Тот же webhook приходит повторно
        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "test-secret")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isOk());

        assertThat(callbackRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll()).hasSize(1);

        OutboxEventEntity outboxEvent =
                outboxRepository.findAll().getFirst();

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(transactionUid);

        assertThat(outboxEvent.getStatus())
                .isEqualTo(OutboxStatus.NEW);
    }
}