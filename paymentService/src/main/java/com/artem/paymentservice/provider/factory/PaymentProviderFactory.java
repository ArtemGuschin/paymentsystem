package com.artem.paymentservice.provider.factory;


import com.artem.paymentservice.provider.PaymentGateway;

public interface PaymentProviderFactory {

    PaymentGateway getProvider(String providerName);

}
