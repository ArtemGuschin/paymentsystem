package com.artem.transactionservice.controller;

import com.artem.transaction.model.TopUpConfirmRequest;
import com.artem.transaction.model.TopUpConfirmResponse;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpInitResponse;
import com.artem.transactionservice.service.TopUpService;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/topup")
@RequiredArgsConstructor
public class TopUpControllerV1 {

    private final TopUpService topUpService;

    @PostMapping("/init")
    public TopUpInitResponse init(@RequestBody TopUpInitRequest request) {
        return topUpService.init(request);
    }

    @PostMapping("/confirm")
    public TopUpConfirmResponse confirm(@RequestBody TopUpConfirmRequest request) {
        return topUpService.confirm(request);
    }
}
