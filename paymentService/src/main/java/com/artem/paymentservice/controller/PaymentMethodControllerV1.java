package com.artem.paymentservice.controller;


import com.artem.paymentservice.api.DefaultApi;
import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentControllerV1 implements DefaultApi {

    private final PaymentMethodService paymentMethodService;

    @Override
    public ResponseEntity<List<PaymentMethodResponse>>
    getAvailablePaymentMethods(
            String currencyCode,
            String countryCode
    ) {

        return ResponseEntity.ok(
                paymentMethodService.getPaymentMethods(
                        currencyCode,
                        countryCode
                )
        );
    }
}