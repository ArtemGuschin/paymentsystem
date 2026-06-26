package com.artem.paymentservice.controller;

import com.artem.paymentservice.api.DefaultApi;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.service.PaymentMethodService;
import com.artem.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentControllerV1 {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse>
    processPayment(
            @RequestBody @Valid PaymentRequest request
    ) {

        return ResponseEntity.ok(
                paymentService.processPayment(request)
        );
    }
}