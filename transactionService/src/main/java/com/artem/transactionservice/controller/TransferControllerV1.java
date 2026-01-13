package com.artem.transactionservice.controller;

import com.artem.transaction.model.TransferConfirmRequest;
import com.artem.transaction.model.TransferConfirmResponse;
import com.artem.transaction.model.TransferInitRequest;
import com.artem.transaction.model.TransferInitResponse;
import com.artem.transactionservice.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
public class TransferControllerV1 {
    private final TransferService transferService;

    @PostMapping("/init")
    public TransferInitResponse init(@RequestBody TransferInitRequest request) {
        return transferService.init(request);
    }

    @PostMapping("/confirm")
    public TransferConfirmResponse confirm(@RequestBody TransferConfirmRequest request) {
        return transferService.confirm(request);
    }


}
