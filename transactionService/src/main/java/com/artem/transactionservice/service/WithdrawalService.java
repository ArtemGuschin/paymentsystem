package com.artem.transactionservice.service;

import com.artem.transaction.model.WithdrawalConfirmRequest;
import com.artem.transaction.model.WithdrawalConfirmResponse;
import com.artem.transaction.model.WithdrawalInitRequest;
import com.artem.transaction.model.WithdrawalInitResponse;

public interface WithdrawalService {
    WithdrawalInitResponse init(WithdrawalInitRequest request);

    WithdrawalConfirmResponse confirm(WithdrawalConfirmRequest request);
}
