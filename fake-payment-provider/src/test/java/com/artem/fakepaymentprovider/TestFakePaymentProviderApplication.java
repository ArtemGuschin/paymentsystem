package com.artem.fakepaymentprovider;

import org.springframework.boot.SpringApplication;

public class TestFakePaymentProviderApplication {

    public static void main(String[] args) {
        SpringApplication.from(FakePaymentProviderApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
