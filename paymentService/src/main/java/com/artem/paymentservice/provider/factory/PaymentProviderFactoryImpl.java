package com.artem.paymentservice.provider.factory;

import com.artem.paymentservice.provider.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactoryImpl
        implements PaymentProviderFactory {

    private final Map<String, PaymentGateway> providers;

    @Override
    public PaymentGateway getProvider(String providerName) {

        PaymentGateway gateway = providers.get(providerName);

        if (gateway == null) {
            throw new IllegalArgumentException(
                    "Unsupported payment provider: " + providerName
            );
        }

        return gateway;
    }
}