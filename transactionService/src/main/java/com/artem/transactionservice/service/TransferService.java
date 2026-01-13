package com.artem.transactionservice.service;

import com.artem.transaction.model.TransferConfirmRequest;
import com.artem.transaction.model.TransferConfirmResponse;
import com.artem.transaction.model.TransferInitRequest;
import com.artem.transaction.model.TransferInitResponse;

public interface TransferService {
    TransferInitResponse init(TransferInitRequest transferInitRequest);

    TransferConfirmResponse confirm(TransferConfirmRequest transferConfirmRequest);
}
