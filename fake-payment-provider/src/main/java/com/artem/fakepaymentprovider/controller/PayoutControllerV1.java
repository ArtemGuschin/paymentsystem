package com.artem.fakepaymentprovider.controller;

import com.artem.fakepaymentprovider.api.PayoutsApi;
import com.artem.fakepaymentprovider.dto.Payout;
import com.artem.fakepaymentprovider.dto.PayoutRequest;
import com.artem.fakepaymentprovider.service.PayoutService;


import lombok.RequiredArgsConstructor;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class PayoutControllerV1 implements PayoutsApi {

    private final PayoutService service;

    @Override
    public ResponseEntity<Payout> createPayout(PayoutRequest request) {
        return ResponseEntity.ok(service.create(request));


    }


}