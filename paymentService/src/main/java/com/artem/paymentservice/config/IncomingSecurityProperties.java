package com.artem.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment-service.security")
public class IncomingSecurityProperties {

    private String username;

    private String passwordHash;
}