package com.artem.paymentservice.config;



import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FakePaymentProviderProperties.class)
public class ConfigurationPropertiesConfig {
}