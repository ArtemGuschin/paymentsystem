package com.artem.individuals.service;

import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.KeycloakTokenResponse;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.dto.response.UserResponse;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {


    private final KeycloakIntegrationClient keycloakClient;
    private final TokenService tokenService;

    public Mono<TokenResponse> registerUser(RegistrationRequest request) {
        return validateRegistrationRequest(request)
                .flatMap(validatedRequest -> keycloakClient.createUserInKeycloak(request.getEmail(), request.getPassword(), request.getFirstName(), request.getLastName(), request.getRole())
                        .then(tokenService.getAccessToken(request.getEmail(), request.getPassword())));
    }

    private Mono<RegistrationRequest> validateRegistrationRequest(RegistrationRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Error: Password and confirm password do not match!!!"));
        }
        return Mono.just(request);
    }

    public Mono<TokenResponse> loginUser(String email, String password) {
        return tokenService.getAccessToken(email, password);
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return tokenService.getRefreshToken(refreshToken);
    }

    public Mono<UserResponse> getUserById(String userId) {
       return keycloakClient.getUserById(userId);
    }

    public Mono<Boolean> userExists(String email) {
        return keycloakClient.userExists(email);
    }
}
