package com.artem.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fake-payment-provider")
public class FakePaymentProviderProperties {

    private String baseUrl;

    private String username;

    private String password;
}