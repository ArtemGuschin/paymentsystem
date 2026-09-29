package com.artem.webhookcollectorservice.controller;

import com.artem.webhookcollectorservice.dto.PaymentProviderWebhookRequest;
import com.artem.webhookcollectorservice.service.SecurityService;
import com.artem.webhookcollectorservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookControllerV1 {

    private final WebhookService webhookService;
    private final SecurityService securityService;

    @PostMapping("/payment-provider")
    public ResponseEntity<Void> receivePaymentProviderWebhook(
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
           @Valid @RequestBody PaymentProviderWebhookRequest request
    ) {

        if (!securityService.isValid(token)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        webhookService.processPaymentProviderCallback(request);

        return ResponseEntity.ok().build();
    }
}