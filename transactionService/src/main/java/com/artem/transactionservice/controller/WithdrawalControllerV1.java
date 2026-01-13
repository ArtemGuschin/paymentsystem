package com.artem.transactionservice.controller;

import com.artem.transaction.model.WithdrawalConfirmRequest;
import com.artem.transaction.model.WithdrawalConfirmResponse;
import com.artem.transaction.model.WithdrawalInitRequest;
import com.artem.transaction.model.WithdrawalInitResponse;
import com.artem.transactionservice.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/withdrawal")
@RequiredArgsConstructor
public class WithdrawalControllerV1 {

    private final WithdrawalService withdrawalService;

    @PostMapping("/init")
    public WithdrawalInitResponse init(
            @RequestBody WithdrawalInitRequest request
    ) {
        return withdrawalService.init(request);
    }

    @PostMapping("/confirm")
    public WithdrawalConfirmResponse confirm(
            @RequestBody WithdrawalConfirmRequest request
    ) {
        return withdrawalService.confirm(request);
    }
}
