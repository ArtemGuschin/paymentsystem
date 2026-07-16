package com.artem.fakepaymentprovider.security;


public final class ServiceTokenProperties {

    public static final String HEADER_NAME = "X-Service-Token";

    public static final String TOKEN = "payment-service-secret";

    private ServiceTokenProperties() {
    }
}