package com.artem.fakepaymentprovider.service;

import com.artem.fakepaymentprovider.dto.Transaction;
import com.artem.fakepaymentprovider.dto.TransactionRequest;
import com.artem.fakepaymentprovider.mapper.TransactionMapper;
import com.artem.fakepaymentprovider.model.MerchantEntity;
import com.artem.fakepaymentprovider.model.TransactionEntity;
import com.artem.fakepaymentprovider.model.WebhookEntity;
import com.artem.fakepaymentprovider.repository.MerchantRepository;
import com.artem.fakepaymentprovider.repository.TransactionRepository;

import com.artem.fakepaymentprovider.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookRepository webhookRepository;
    private final TransactionMapper mapper;

    @Value("${webhook.security.token}")
    private String webhookSecurityToken;

    @Value("${webhook.collector-url}")
    private String webhookCollectorUrl;


    @Transactional
    public Transaction create(TransactionRequest transactionRequest) {

        MerchantEntity merchant = getCurrentMerchant();

        if (transactionRequest.getExternalId() != null) {
            Optional<TransactionEntity> existing =
                    transactionRepository.findByMerchant_IdAndExternalId(
                            merchant.getId(),
                            transactionRequest.getExternalId()
                    );

            if (existing.isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transaction with this externalId already exists"
                );
            }
        }

        TransactionEntity entity = mapper.toEntity(transactionRequest);

        entity.setMerchant(merchant);
        entity.setStatus("PENDING");
        entity.setCreatedAt(Instant.now());

        TransactionEntity saved = transactionRepository.save(entity);
        processAsync(saved);

        return mapper.toDto(saved);
    }


    @Transactional(readOnly = true)
    public Transaction getById(Long id) {

        MerchantEntity merchant = getCurrentMerchant();

        return transactionRepository.findById(id)
                .filter(tx -> tx.getMerchant().getId().equals(merchant.getId()))
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    @Transactional(readOnly = true)
    public Transaction getByExternalId(String externalId) {

        MerchantEntity merchant = getCurrentMerchant();

        return transactionRepository
                .findByMerchant_IdAndExternalId(
                        merchant.getId(),
                        externalId
                )
                .map(mapper::toDto)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Transaction not found"
                        )
                );
    }


    @Transactional(readOnly = true)
    public List<Transaction> getAll(OffsetDateTime startDate, OffsetDateTime endDate) {

        MerchantEntity merchant = getCurrentMerchant();

        return transactionRepository
                .findByMerchant_IdAndCreatedAtBetween(
                        merchant.getId(),
                        startDate.toInstant(),
                        endDate.toInstant()
                )
                .stream()
                .map(mapper::toDto)
                .toList();
    }


    private MerchantEntity getCurrentMerchant() {

        String principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // вызов пришел от PaymentService
        if ("payment-service".equals(principal)) {

            return merchantRepository.findByMerchantId("merchant_001")
                    .orElseThrow(() ->
                            new RuntimeException("Service merchant not found"));
        }

        // обычный вызов по Basic Auth
        return merchantRepository.findByMerchantId(principal)
                .orElseThrow(() ->
                        new RuntimeException("Merchant not found"));
    }

    private void processAsync(TransactionEntity tx) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);

                tx.setStatus("SUCCESS");
                tx.setUpdatedAt(Instant.now());
                
                transactionRepository.save(tx);

                sendWebhook(tx);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendWebhook(TransactionEntity tx) {

        if (tx.getNotificationUrl() == null) {
            System.out.println(">>> NO WEBHOOK URL");
            return;
        }

        /*
         * Security boundary:
         * общий секрет Webhook Collector разрешено отправлять
         * только на заранее доверенный адрес из конфигурации.
         */
        if (!webhookCollectorUrl.equals(tx.getNotificationUrl())) {
            throw new IllegalArgumentException(
                    "Untrusted webhook notification URL"
            );
        }

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionUid", tx.getExternalId());
        payload.put("status", tx.getStatus());
        payload.put("amount", tx.getAmount());

        WebhookEntity webhook = WebhookEntity.builder()
                .eventType("TRANSACTION_SUCCESS")
                .entityId(tx.getId())
                .payload(payload)
                .notificationUrl(tx.getNotificationUrl())
                .receivedAt(Instant.now())
                .build();

        webhook = webhookRepository.save(webhook);

        try {
            System.out.println(
                    ">>> SENDING WEBHOOK TO TRUSTED COLLECTOR: "
                            + tx.getNotificationUrl()
            );

            Map<String, Object> body = new HashMap<>();
            body.put("eventType", "TRANSACTION_SUCCESS");
            body.put("entityId", tx.getId());
            body.put("payload", payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            /*
             * Секрет добавляется только после проверки,
             * что destination является trusted Collector.
             */
            headers.set(
                    "X-Webhook-Token",
                    webhookSecurityToken
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            restTemplate.postForEntity(
                    tx.getNotificationUrl(),
                    request,
                    Void.class
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        webhookRepository.save(webhook);
    }


}