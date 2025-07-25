package com.artem.individuals.client;

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
public class KeycloakIntegrationClient {

    private final Keycloak keycloak;
    private final WebClient webClient;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    public Mono<TokenResponse> registerUser(RegistrationRequest request) {
        return createUserInKeycloak(request.getEmail(), request.getPassword(), request.getFirstName(), request.getLastName(), request.getRole())
                .then(loginUser(request.getEmail(), request.getPassword()));
    }

    public Mono<Void> createUserInKeycloak(String email, String password, String firstName, String lastName, String role) {
        return Mono.fromCallable(() -> {
            // 1. Создаем представление пользователя
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmailVerified(true);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            // 2. Создаем пользователя в Keycloak
            try (Response response = keycloak.realm(realm).users().create(user)) {
                if (response.getStatus() != 201) {
                    throw new RuntimeException("Failed to create user: " + response.getStatusInfo());
                }

                // 3. Извлекаем ID созданного пользователя из Location header
                String location = response.getLocation().getPath();
                String userId = location.substring(location.lastIndexOf('/') + 1);

                // 4. Получаем объект роли
                RoleRepresentation roleRep = keycloak.realm(realm).roles()
                        .get(role)
                        .toRepresentation();

                // 5. Назначаем роль пользователю
                keycloak.realm(realm).users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .add(Collections.singletonList(roleRep));
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }



    public Mono<TokenResponse> loginUser(String email, String password) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("username", email)
                        .with("password", password))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(error ->
                                Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials: " + error))))
                .bodyToMono(KeycloakTokenResponse.class)
                .map(kcResponse -> new TokenResponse(
                        kcResponse.getAccessToken(),
                        kcResponse.getExpiresIn(),
                        kcResponse.getRefreshToken(),
                        kcResponse.getTokenType()));
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")))
                .bodyToMono(KeycloakTokenResponse.class)
                .map(kcResponse -> new TokenResponse(
                        kcResponse.getAccessToken(),
                        kcResponse.getExpiresIn(),
                        kcResponse.getRefreshToken(),
                        kcResponse.getTokenType()));
    }

    public Mono<UserResponse> getUserById(String userId) {
        return Mono.fromCallable(() -> {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            List<String> roles = userResource.roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());

            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    roles,
                    Instant.ofEpochMilli(user.getCreatedTimestamp()).toString()
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> userExists(String email) {
        return Mono.fromCallable(() ->
                        !keycloak.realm(realm).users().searchByEmail(email, true).isEmpty())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
