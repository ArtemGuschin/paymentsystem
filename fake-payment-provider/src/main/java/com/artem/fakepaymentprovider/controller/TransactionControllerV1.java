package com.artem.fakepaymentprovider.controller;


import com.artem.fakepaymentprovider.api.TransactionsApi;
import com.artem.fakepaymentprovider.dto.Transaction;
import com.artem.fakepaymentprovider.dto.TransactionRequest;
import com.artem.fakepaymentprovider.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;


@RestController
@RequiredArgsConstructor
public class TransactionControllerV1 implements TransactionsApi {

    private final TransactionService service;

    @Override
    public ResponseEntity<Transaction> createTransaction(
            @Valid @RequestBody TransactionRequest transactionRequest
    ) {
        System.out.println(">>> HIT CREATE TRANSACTION");

        Transaction response = service.create(transactionRequest);

        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<Transaction> getTransactionById(Long id) {

        Transaction response = service.getById(id);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<Transaction>> getTransactions(
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {

        List<Transaction> response =
                service.getAll(startDate, endDate);

        return ResponseEntity.ok(response);
    }
}

