package com.artem.transactionservice.service;

import com.artem.transaction.model.TopUpConfirmRequest;
import com.artem.transaction.model.TopUpConfirmResponse;
import com.artem.transaction.model.TopUpInitRequest;
import com.artem.transaction.model.TopUpInitResponse;

public interface TopUpService {
    TopUpInitResponse init(TopUpInitRequest request);

    TopUpConfirmResponse confirm(TopUpConfirmRequest request);
}
