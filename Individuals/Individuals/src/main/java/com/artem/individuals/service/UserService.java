package com.artem.individuals.service;

import com.artem.api.UsersApi;
import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.dto.response.UserResponse;
import com.artem.individuals.kafka.NotificationEventProducer;
import com.artem.model.UserCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakIntegrationClient keycloakClient;
    private final TokenService tokenService;
    private final UsersApi usersApi;
    private final NotificationEventProducer notificationEventProducer;

    public Mono<TokenResponse> registerUser(RegistrationRequest request) {
        return validateRegistrationRequest(request)
                .flatMap(r -> {
                    UserCreateRequest userCreateRequest = new UserCreateRequest();
                    userCreateRequest.setEmail(request.getEmail());
                    userCreateRequest.setPassword(request.getPassword());
                    userCreateRequest.setFirstName(request.getFirstName());
                    userCreateRequest.setLastName(request.getLastName());
                    userCreateRequest.setAddress(request.getAddress());
                    userCreateRequest.setIndividual(request.getIndividual());

                    // 1. Создаем пользователя в Person Service
                    return Mono.fromCallable(() -> usersApi.createUser(userCreateRequest))
                            .flatMap(userResponse ->

                                    // 2. Создаем пользователя в Keycloak
                                    keycloakClient.createUserInKeycloak(
                                                    request.getEmail(),
                                                    request.getPassword(),
                                                    request.getFirstName(),
                                                    request.getLastName(),
                                                    request.getRole()
                                            )

                                            // createUserInKeycloak возвращает Mono<Void>,
                                            // поэтому здесь нужен then(), а не flatMap()
                                            .then(
                                                    tokenService.getAccessToken(
                                                            request.getEmail(),
                                                            request.getPassword()
                                                    )
                                            )

                                            // 3. После успешного получения token
                                            // публикуем notification.created
                                            .flatMap(tokenResponse ->
                                                    notificationEventProducer
                                                            .sendUserRegisteredNotification(
                                                                    userResponse.getId(),
                                                                    request.getEmail()
                                                            )
                                                            .onErrorResume(error -> {
                                                                log.error(
                                                                        "Registration completed, but notification event publishing failed for userUid={}",
                                                                        userResponse.getId(),
                                                                        error
                                                                );

                                                                return Mono.empty();
                                                            })
                                                            .thenReturn(tokenResponse)
                                            )

                                            // 4. Если Keycloak или получение token упали,
                                            // компенсируем Person Service
                                            .onErrorResume(throwable -> {
                                                log.error(
                                                        "Registration flow failed, compensating Person Service",
                                                        throwable
                                                );

                                                return Mono.fromCallable(() -> {
                                                            log.info(
                                                                    "Deleting user {} from Person Service as compensation",
                                                                    userResponse.getId()
                                                            );

                                                            usersApi.deleteUser(userResponse.getId());

                                                            return userResponse;
                                                        })
                                                        .then(Mono.error(throwable));
                                            })
                            );
                });
    }

    private Mono<RegistrationRequest> validateRegistrationRequest(RegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Error: Password and confirm password do not match!!!"
                    )
            );
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