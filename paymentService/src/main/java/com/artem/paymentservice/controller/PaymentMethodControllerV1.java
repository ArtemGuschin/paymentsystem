package com.artem.paymentservice.controller;


import com.artem.paymentservice.api.DefaultApi;
import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodControllerV1 {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/{currencyCode}/{countryCode}")
    public ResponseEntity<List<PaymentMethodResponse>>
    getAvailablePaymentMethods(
            @PathVariable String currencyCode,
            @PathVariable String countryCode
    ) {

        return ResponseEntity.ok(
                paymentMethodService.getAvailablePaymentMethods(
                        currencyCode,
                        countryCode
                )
        );
    }
}