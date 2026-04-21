package com.artem.fakepaymentprovider.controller;

import com.artem.fakepaymentprovider.api.WebhooksApi;
import com.artem.fakepaymentprovider.dto.Webhook;
import com.artem.fakepaymentprovider.service.WebhookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WebhookControllerV1 implements WebhooksApi {

    private final WebhookService service;

    @Override
    public ResponseEntity<Void> receiveWebhook(Webhook request) {
        service.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}