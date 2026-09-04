package com.artem.paymentservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        FakePaymentProviderProperties.class,
        IncomingSecurityProperties.class
})
public class ConfigurationPropertiesConfig {
}