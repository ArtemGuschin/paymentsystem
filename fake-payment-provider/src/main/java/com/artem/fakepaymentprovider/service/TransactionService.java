package com.artem.fakepaymentprovider.service;

import com.artem.fakepaymentprovider.dto.Transaction;
import com.artem.fakepaymentprovider.dto.TransactionRequest;
import com.artem.fakepaymentprovider.mapper.TransactionMapper;
import com.artem.fakepaymentprovider.model.MerchantEntity;
import com.artem.fakepaymentprovider.model.TransactionEntity;
import com.artem.fakepaymentprovider.repository.MerchantRepository;
import com.artem.fakepaymentprovider.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
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
    private final TransactionMapper mapper;


    @Transactional
    public Transaction create(TransactionRequest transactionRequest) {

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

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

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        return transactionRepository.findById(id)
                .filter(tx -> tx.getMerchant().getId().equals(merchant.getId()))
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }


    @Transactional(readOnly = true)
    public List<Transaction> getAll(OffsetDateTime startDate, OffsetDateTime endDate) {

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

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


    private String getCurrentMerchantId() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
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

        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> body = new HashMap<>();
            body.put("eventType", "TRANSACTION_SUCCESS");
            body.put("entityId", tx.getId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", tx.getStatus());
            payload.put("amount", tx.getAmount());

            body.put("payload", payload);

            System.out.println(">>> SENDING WEBHOOK TO: " + tx.getNotificationUrl());

            restTemplate.postForObject(
                    tx.getNotificationUrl(),
                    body,
                    String.class
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}