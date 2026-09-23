package com.artem.webhookcollectorservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService {

    @Value("${webhook.security.token}")
    private String expectedToken;

    public boolean isValid(String token) {
        return expectedToken.equals(token);
    }
}