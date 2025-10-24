package com.artem.individuals.service;


import com.artem.api.UsersApi;
import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.dto.response.UserResponse;
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

                    // 1. Сначала создаем в Person Service
                    return Mono.fromCallable(() -> usersApi.createUser(userCreateRequest))
                            .flatMap(userResponse ->
                                    // 2. Потом создаем в Keycloak
                                    keycloakClient.createUserInKeycloak(request.getEmail(),
                                                    request.getPassword(),
                                                    request.getFirstName(),
                                                    request.getLastName(),
                                                    request.getRole())
                                            .flatMap(keycloakUserId ->
                                                    // 3. Если оба успешны - возвращаем токен
                                                    tokenService.getAccessToken(request.getEmail(), request.getPassword())
                                            )
                                            .onErrorResume(throwable -> {
                                                log.error("Keycloak error during registration, compensating...", throwable);
                                                // КОМПЕНСАЦИЯ: если Keycloak упал - удаляем из Person Service
                                                return Mono.fromCallable(() -> {
                                                            log.info("Deleting user {} from Person Service as compensation", userResponse.getId());
                                                            usersApi.deleteUser(userResponse.getId()); // удаляем пользователя
                                                            return userResponse;
                                                        })
                                                        .then(Mono.error(throwable)); // пробрасываем оригинальную ошибку
                                            })
                            );
                });
    }

    private Mono<RegistrationRequest> validateRegistrationRequest(RegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
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
