package com.artem.paymentservice.service;


import com.artem.paymentservice.dto.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {

    List<PaymentMethodResponse> getPaymentMethods(
            String currencyCode,
            String countryCode
    );
}