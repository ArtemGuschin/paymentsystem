package com.artem.webhookcollectorservice.integration;

import com.artem.webhookcollectorservice.TestcontainersConfiguration;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.entity.PaymentProviderCallbackEntity;
import com.artem.webhookcollectorservice.outbox.OutboxStatus;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import com.artem.webhookcollectorservice.repository.PaymentProviderCallbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.artem.webhookcollectorservice.entity.UnknownCallbackEntity;
import com.artem.webhookcollectorservice.repository.UnknownCallbackRepository;

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

    @Autowired
    private UnknownCallbackRepository unknownCallbackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        callbackRepository.deleteAll();
        unknownCallbackRepository.deleteAll();
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
    @Test
    void shouldStoreUnknownEventWithoutCreatingCallbackOrOutbox() throws Exception {

        UUID transactionUid =
                UUID.fromString("850e8400-e29b-41d4-a716-446655440000");

        String body = """
            {
              "eventType": "UNSUPPORTED_EVENT",
              "entityId": 60,
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

        List<UnknownCallbackEntity> unknownCallbacks =
                unknownCallbackRepository.findAll();

        assertThat(unknownCallbacks).hasSize(1);

        assertThat(unknownCallbacks.getFirst().getBody())
                .contains("UNSUPPORTED_EVENT")
                .contains(transactionUid.toString());

        assertThat(callbackRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestAndSaveNothingWhenPayloadIsMissing() throws Exception {

        String body = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 61
            }
            """;

        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "test-secret")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isBadRequest());

        assertThat(callbackRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
        assertThat(unknownCallbackRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectInvalidStatusWithoutBlockingCorrectedWebhook() throws Exception {

        UUID transactionUid =
                UUID.fromString("950e8400-e29b-41d4-a716-446655440000");

        // 1. Приходит известное событие, но с недопустимым статусом.
        String invalidBody = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 62,
              "payload": {
                "transactionUid": "%s",
                "status": "BANANA",
                "amount": 100.00
              }
            }
            """.formatted(transactionUid);

        mockMvc.perform(
                        post("/api/v1/webhooks/payment-provider")
                                .header("X-Webhook-Token", "test-secret")
                                .contentType("application/json")
                                .content(invalidBody)
                )
                .andExpect(status().isBadRequest());

        // Невалидный webhook не должен ничего занимать в БД.
        assertThat(callbackRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
        assertThat(unknownCallbackRepository.findAll()).isEmpty();

        // 2. Provider исправляет webhook.
        // entityId и eventType намеренно ТЕ ЖЕ.
        String correctedBody = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 62,
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
                                .content(correctedBody)
                )
                .andExpect(status().isOk());

        // 3. Исправленная доставка должна обработаться как первая валидная.
        assertThat(callbackRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(unknownCallbackRepository.findAll()).isEmpty();

        PaymentProviderCallbackEntity callback =
                callbackRepository.findAll().getFirst();

        assertThat(callback.getProviderTransactionId()).isEqualTo(62L);
        assertThat(callback.getType()).isEqualTo("TRANSACTION_SUCCESS");

        OutboxEventEntity outboxEvent =
                outboxRepository.findAll().getFirst();

        assertThat(outboxEvent.getAggregateId()).isEqualTo(transactionUid);
        assertThat(outboxEvent.getPayload()).contains("SUCCESS");
    }

    @Test
    void shouldStoreWebhookBodyAsPlainTextInPostgres() throws Exception {

        UUID transactionUid =
                UUID.fromString("a50e8400-e29b-41d4-a716-446655440000");

        String body = """
            {
              "eventType": "TRANSACTION_SUCCESS",
              "entityId": 63,
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

        String storedBody = jdbcTemplate.queryForObject(
                """
                SELECT body
                FROM payment_provider_callbacks
                WHERE provider_transaction_id = ?
                """,
                String.class,
                63L
        );

        assertThat(storedBody)
                .isNotNull()
                .contains("\"eventType\":\"TRANSACTION_SUCCESS\"")
                .contains("\"entityId\":63")
                .contains(transactionUid.toString())
                .contains("\"status\":\"SUCCESS\"");
    }




}