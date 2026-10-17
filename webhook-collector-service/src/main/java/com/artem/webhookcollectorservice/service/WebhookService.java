package com.artem.webhookcollectorservice.service;

import com.artem.webhookcollectorservice.dto.NotificationOutboxPayload;
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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String PROVIDER =
            "FAKE_PAYMENT_PROVIDER";

    private static final String PAYMENT_STATUS_UPDATED =
            "payment.status.updated";

    private static final String NOTIFICATION_CREATED =
            "notification.created";

    private static final String TRANSACTION_SUCCESS =
            "TRANSACTION_SUCCESS";

    private static final String PAYMENT_SUCCEEDED =
            "PAYMENT_SUCCEEDED";

    private static final String SYSTEM =
            "SYSTEM";

    private final PaymentProviderCallbackRepository callbackRepository;
    private final OutboxRepository outboxRepository;
    private final UnknownCallbackRepository unknownCallbackRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentProviderCallback(
            PaymentProviderWebhookRequest request
    ) {
        try {

            String body =
                    objectMapper.writeValueAsString(
                            request
                    );

            /*
             * Неизвестные типы webhook сохраняем отдельно.
             * Kafka-события по ним не создаём.
             */
            if (!TRANSACTION_SUCCESS.equals(
                    request.getEventType()
            )) {

                unknownCallbackRepository.save(
                        new UnknownCallbackEntity(
                                body
                        )
                );

                return;
            }

            /*
             * Для TRANSACTION_SUCCESS ожидаем только SUCCESS.
             */
            if (!"SUCCESS".equals(
                    request.getPayload().getStatus()
            )) {

                throw new InvalidWebhookException(
                        "Status "
                                + request.getPayload().getStatus()
                                + " is not valid for event type "
                                + request.getEventType()
                );
            }

            /*
             * Atomic idempotency.
             *
             * Один и тот же provider callback может быть
             * сохранён только один раз.
             */
            int inserted =
                    callbackRepository.insertIfAbsent(
                            UUID.randomUUID(),
                            body,
                            request.getEntityId(),
                            request.getEventType(),
                            PROVIDER
                    );

            /*
             * Duplicate webhook.
             *
             * Никаких новых outbox-событий не создаём.
             */
            if (inserted == 0) {
                return;
            }

            UUID transactionUid =
                    request.getPayload()
                            .getTransactionUid();

            /*
             * =================================================
             * 1. payment.status.updated
             * =================================================
             */
            PaymentStatusUpdatedEvent paymentStatusEvent =
                    new PaymentStatusUpdatedEvent(
                            transactionUid,
                            request.getPayload().getStatus()
                    );

            String paymentStatusPayload =
                    objectMapper.writeValueAsString(
                            paymentStatusEvent
                    );

            OutboxEventEntity paymentStatusOutboxEvent =
                    new OutboxEventEntity(
                            transactionUid,
                            PAYMENT_STATUS_UPDATED,
                            paymentStatusPayload
                    );

            outboxRepository.save(
                    paymentStatusOutboxEvent
            );

            /*
             * =================================================
             * 2. notification.created
             * =================================================
             *
             * userUid здесь намеренно ещё нет.
             *
             * OutboxPublisher получит его позднее
             * из Transaction Service по transactionUid.
             */
            NotificationOutboxPayload notificationPayload =
                    new NotificationOutboxPayload(
                            UUID.randomUUID().toString(),
                            PAYMENT_SUCCEEDED,
                            buildSuccessMessage(request),
                            transactionUid.toString(),
                            null,
                            "Payment completed",
                            SYSTEM,
                            Instant.now().toEpochMilli()
                    );

            String notificationJson =
                    objectMapper.writeValueAsString(
                            notificationPayload
                    );

            OutboxEventEntity notificationOutboxEvent =
                    new OutboxEventEntity(
                            transactionUid,
                            NOTIFICATION_CREATED,
                            notificationJson
                    );

            outboxRepository.save(
                    notificationOutboxEvent
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to serialize webhook event",
                    e
            );
        }
    }

    private String buildSuccessMessage(
            PaymentProviderWebhookRequest request
    ) {
        return "Payment provider confirmed transaction "
                + request.getPayload().getTransactionUid()
                + " successfully. Amount: "
                + request.getPayload().getAmount();
    }
}