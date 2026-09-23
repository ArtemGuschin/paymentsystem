package com.artem.webhookcollectorservice.service;

import com.artem.webhookcollectorservice.dto.PaymentProviderWebhookRequest;
import com.artem.webhookcollectorservice.dto.PaymentStatusUpdatedEvent;
import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.entity.PaymentProviderCallbackEntity;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import com.artem.webhookcollectorservice.repository.PaymentProviderCallbackRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String PROVIDER = "FAKE_PAYMENT_PROVIDER";
    private static final String PAYMENT_STATUS_UPDATED = "payment.status.updated";

    private final PaymentProviderCallbackRepository callbackRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentProviderCallback(
            PaymentProviderWebhookRequest request
    ) {
        boolean alreadyProcessed =
                callbackRepository.existsByProviderAndProviderTransactionIdAndType(
                        PROVIDER,
                        request.getEntityId(),
                        request.getEventType()
                );

        if (alreadyProcessed) {
            return;
        }


        try {
            String body = objectMapper.writeValueAsString(request);

            PaymentProviderCallbackEntity callback =
                    new PaymentProviderCallbackEntity(
                            body,
                            request.getEntityId(),
                            request.getEventType(),
                            PROVIDER
                    );

            callbackRepository.save(callback);

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