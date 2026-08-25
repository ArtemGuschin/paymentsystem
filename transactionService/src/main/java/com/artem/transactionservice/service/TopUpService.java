package com.artem.transactionservice.service;

import com.artem.transaction.model.TopUpCompleteRequest;
import com.artem.transaction.model.TopUpConfirmRequest;
import com.artem.transaction.model.TopUpConfirmResponse;
import com.artem.transaction.model.TopUpFailRequest;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpInitResponse;
import com.artem.transaction.model.TopUpResultResponse;

import java.util.UUID;

public interface TopUpService {

    TopUpInitResponse init(TopUpInitRequest request);

    TopUpConfirmResponse confirm(TopUpConfirmRequest request);

    TopUpResultResponse completeTopUp(
            UUID transactionUid,
            TopUpCompleteRequest request
    );

    TopUpResultResponse failTopUp(
            UUID transactionUid,
            TopUpFailRequest request
    );
}