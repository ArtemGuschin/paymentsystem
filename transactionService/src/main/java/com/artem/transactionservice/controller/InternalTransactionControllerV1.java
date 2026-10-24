package com.artem.transactionservice.controller;

import com.artem.transactionservice.dto.NotificationContextResponse;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/internal/transactions")
@RequiredArgsConstructor
public class InternalTransactionControllerV1 {

    private final TransactionRepository transactionRepository;

    @GetMapping("/{transactionUid}/notification-context")
    public NotificationContextResponse getNotificationContext(
            @PathVariable UUID transactionUid
    ) {
        Transaction transaction = transactionRepository
                .findById(transactionUid)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Transaction not found: " + transactionUid
                        )
                );

        return new NotificationContextResponse(
                transaction.getUid(),
                transaction.getUserUid()
        );
    }
}