package com.artem.fakepaymentprovider.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestWebhookControllerV1 {

    @PostMapping("/test-webhook")
    public void webhook(@RequestBody String body) {
        System.out.println(">>> WEBHOOK RECEIVED: " + body);
    }
}