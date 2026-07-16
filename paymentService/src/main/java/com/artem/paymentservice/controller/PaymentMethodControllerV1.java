package com.artem.paymentservice.controller;


import com.artem.paymentservice.api.PaymentMethodsApi;
import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentMethodControllerV1 implements PaymentMethodsApi {

    private final PaymentMethodService paymentMethodService;

    @Override
    public ResponseEntity<List<PaymentMethodResponse>>
    getAvailablePaymentMethods(
            String currencyCode,
            String countryCode) {

        return ResponseEntity.ok(
                paymentMethodService.getAvailablePaymentMethods(
                        currencyCode,
                        countryCode
                )
        );
    }
}