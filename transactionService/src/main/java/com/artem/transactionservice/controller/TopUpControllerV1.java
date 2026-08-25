package com.artem.transactionservice.controller;

import com.artem.transaction.model.TopUpCompleteRequest;
import com.artem.transaction.model.TopUpConfirmRequest;
import com.artem.transaction.model.TopUpConfirmResponse;
import com.artem.transaction.model.TopUpFailRequest;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpInitResponse;
import com.artem.transaction.model.TopUpResultResponse;
import com.artem.transactionservice.service.TopUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topup")
@RequiredArgsConstructor
public class TopUpControllerV1 {

    private final TopUpService topUpService;

    @PostMapping("/init")
    public TopUpInitResponse init(
            @RequestBody TopUpInitRequest request
    ) {
        return topUpService.init(request);
    }

    @PostMapping("/confirm")
    public ResponseEntity<TopUpConfirmResponse> confirm(
            @RequestBody TopUpConfirmRequest request
    ) {
        return ResponseEntity
                .status(202)
                .body(topUpService.confirm(request));
    }

    @PostMapping("/{transactionUid}/complete")
    public TopUpResultResponse complete(
            @PathVariable UUID transactionUid,
            @RequestBody TopUpCompleteRequest request
    ) {
        return topUpService.completeTopUp(
                transactionUid,
                request
        );
    }

    @PostMapping("/{transactionUid}/fail")
    public TopUpResultResponse fail(
            @PathVariable UUID transactionUid,
            @RequestBody(required = false) TopUpFailRequest request
    ) {
        return topUpService.failTopUp(
                transactionUid,
                request
        );
    }
}