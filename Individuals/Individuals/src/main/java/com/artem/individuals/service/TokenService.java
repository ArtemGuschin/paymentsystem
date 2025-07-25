package com.artem.individuals.service;

import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeycloakIntegrationClient keycloakIntegrationClient;

    public Mono<TokenResponse> getAccessToken(String email, String password) {
        return keycloakIntegrationClient.loginUser(email, password);
    }

    public Mono<TokenResponse> getRefreshToken(String refreshToken) {
      return keycloakIntegrationClient.refreshToken(refreshToken);
    }
}
