package com.artem.fakepaymentprovider.service;

import com.artem.fakepaymentprovider.dto.Webhook;
import com.artem.fakepaymentprovider.model.WebhookEntity;
import com.artem.fakepaymentprovider.repository.PayoutRepository;
import com.artem.fakepaymentprovider.repository.TransactionRepository;
import com.artem.fakepaymentprovider.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;
    private final RestTemplate restTemplate;

    // =========================
    // 📥 ПРИЁМ WEBHOOK
    // =========================
    @Transactional
    public void handleWebhook(Webhook request) {

        log.info("Received webhook: eventType={}, entityId={}",
                request.getEventType(), request.getEntityId());

        WebhookEntity webhook = WebhookEntity.builder()
                .eventType(request.getEventType())
                .entityId(request.getEntityId())
                .payload(request.getPayload())
                .notificationUrl(request.getNotificationUrl())
                .receivedAt(Instant.now())
                .build();

        webhookRepository.save(webhook);

        String status = resolveStatus(request.getEventType());

        if (request.getEventType().startsWith("TRANSACTION_")) {

            transactionRepository.findById(request.getEntityId())
                    .ifPresent(tx -> {
                        tx.setStatus(status);
                        tx.setUpdatedAt(Instant.now());
                    });

        } else if (request.getEventType().startsWith("PAYOUT_")) {

            payoutRepository.findById(request.getEntityId())
                    .ifPresent(payout -> {
                        payout.setStatus(status);
                        payout.setUpdatedAt(Instant.now());
                    });
        }
    }

    // =========================
    // 📤 ОТПРАВКА WEBHOOK
    // =========================
    public void sendWebhook(String eventType, Long entityId, Object payload, String url) {

        log.info("Sending webhook: {} to {}", eventType, url);

        try {
            restTemplate.postForEntity(url, payload, Void.class); 
        } catch (Exception e) {
            log.error("Webhook failed: {}", e.getMessage());
        }
    }

    private String resolveStatus(String eventType) {
        if (eventType.endsWith("SUCCESS")) return "SUCCESS";
        if (eventType.endsWith("FAILED")) return "FAILED";
        return "PENDING";
    }
}

