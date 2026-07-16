package com.artem.paymentservice.controller;


import com.artem.paymentservice.api.PaymentsApi;
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
public class PaymentControllerV1  implements PaymentsApi {

    private final PaymentService paymentService;

    @Override
    public ResponseEntity<PaymentResponse> processPayment(
            PaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.processPayment(request)
        );
    }
}