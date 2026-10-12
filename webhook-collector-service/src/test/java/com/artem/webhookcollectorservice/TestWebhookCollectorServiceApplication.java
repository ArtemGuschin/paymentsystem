package com.artem.webhookcollectorservice;

import org.springframework.boot.SpringApplication;

public class TestWebhookCollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(WebhookCollectorServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
