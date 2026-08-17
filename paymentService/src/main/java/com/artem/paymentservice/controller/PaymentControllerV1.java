package com.artem.paymentservice.controller;

import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;


import com.artem.paymentservice.api.PaymentsApi;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class PaymentControllerV1
        implements PaymentsApi {

    private final PaymentService paymentService;


    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<PaymentResponse> processPayment(
            PaymentRequest paymentRequest
    ) {

        return ResponseEntity.ok(
                paymentService.processPayment(paymentRequest)
        );
    }


}