package com.artem.webhookcollectorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebhookCollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookCollectorServiceApplication.class, args);
    }
}
