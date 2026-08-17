package com.artem.fakepaymentprovider.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "service-token")
public class ServiceTokenProperties {

    private String headerName;

    private String token;

}
