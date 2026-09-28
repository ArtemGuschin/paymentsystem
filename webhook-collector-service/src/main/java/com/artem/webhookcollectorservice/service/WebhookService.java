package com.artem.webhookcollectorservice.service;

import com.artem.webhookcollectorservice.dto.PaymentProviderWebhookRequest;
import com.artem.webhookcollectorservice.dto.PaymentStatusUpdatedEvent;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.entity.UnknownCallbackEntity;
import com.artem.webhookcollectorservice.exception.InvalidWebhookException;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import com.artem.webhookcollectorservice.repository.PaymentProviderCallbackRepository;
import com.artem.webhookcollectorservice.repository.UnknownCallbackRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String PROVIDER = "FAKE_PAYMENT_PROVIDER";
    private static final String PAYMENT_STATUS_UPDATED = "payment.status.updated";
    private static final String TRANSACTION_SUCCESS = "TRANSACTION_SUCCESS";

    private final PaymentProviderCallbackRepository callbackRepository;
    private final OutboxRepository outboxRepository;
    private final UnknownCallbackRepository unknownCallbackRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentProviderCallback(
            PaymentProviderWebhookRequest request
    ) {
        try {
            String body = objectMapper.writeValueAsString(request);

            // Unknown event types must be stored for investigation,
            // but must never produce a payment status event.
            if (!TRANSACTION_SUCCESS.equals(request.getEventType())) {
                unknownCallbackRepository.save(
                        new UnknownCallbackEntity(body)
                );
                return;
            }

            // Known event must contain a status valid for this event type.
            if (!"SUCCESS".equals(request.getPayload().getStatus())) {
                throw new InvalidWebhookException(
                        "Status " + request.getPayload().getStatus()
                                + " is not valid for event type "
                                + request.getEventType()
                );
            }

            // Atomic idempotency:
            // only one concurrent request can insert the callback.
            int inserted = callbackRepository.insertIfAbsent(
                    UUID.randomUUID(),
                    body,
                    request.getEntityId(),
                    request.getEventType(),
                    PROVIDER
            );

            // Duplicate webhook was already accepted.
            // Do not create another outbox event.
            if (inserted == 0) {
                return;
            }

            PaymentStatusUpdatedEvent event =
                    new PaymentStatusUpdatedEvent(
                            request.getPayload().getTransactionUid(),
                            request.getPayload().getStatus()
                    );

            String eventPayload =
                    objectMapper.writeValueAsString(event);

            OutboxEventEntity outboxEvent =
                    new OutboxEventEntity(
                            request.getPayload().getTransactionUid(),
                            PAYMENT_STATUS_UPDATED,
                            eventPayload
                    );

            outboxRepository.save(outboxEvent);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize webhook event",
                    e
            );
        }
    }
}