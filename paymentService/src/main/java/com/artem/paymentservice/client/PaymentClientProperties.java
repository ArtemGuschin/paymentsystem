package com.artem.paymentservice.client;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "payment-service")
public class PaymentClientProperties {

    /**
     * Base URL Payment Service
     */
    private String url;

    /**
     * Connection timeout
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Read timeout
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}