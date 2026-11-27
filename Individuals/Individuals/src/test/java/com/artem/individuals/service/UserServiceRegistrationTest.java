
package com.artem.individuals.service;

import com.artem.api.UsersApi;
import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.outbox.NotificationOutboxService;
import com.artem.model.UserCreateRequest;
import com.artem.model.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceRegistrationTest {

    private static final UUID USER_UID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";

    private KeycloakIntegrationClient keycloakClient;
    private TokenService tokenService;
    private UsersApi usersApi;
    private NotificationOutboxService outboxService;
    private UserService userService;

    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        keycloakClient = mock(KeycloakIntegrationClient.class);
        tokenService = mock(TokenService.class);
        usersApi = mock(UsersApi.class);
        outboxService = mock(NotificationOutboxService.class);

        userService = new UserService(
                keycloakClient,
                tokenService,
                usersApi,
                outboxService
        );

        UserResponse personUser = mock(UserResponse.class);

        when(personUser.getId()).thenReturn(USER_UID);

        when(usersApi.createUser(any(UserCreateRequest.class)))
                .thenReturn(personUser);

        when(keycloakClient.createUserInKeycloak(
                EMAIL, PASSWORD, "Test", "User", "user"
        )).thenReturn(Mono.empty());

        tokenResponse = new TokenResponse(
                "access-token",
                3600,
                "refresh-token",
                "Bearer"
        );

        when(tokenService.getAccessToken(EMAIL, PASSWORD))
                .thenReturn(Mono.just(tokenResponse));
    }

    private RegistrationRequest registrationRequest() {
        RegistrationRequest request = new RegistrationRequest();

        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("user");

        return request;
    }

    @Test
    void shouldSaveOutboxAfterSuccessfulRegistration() {
        when(outboxService.saveUserRegistered(USER_UID, EMAIL))
                .thenReturn(UUID.randomUUID().toString());

        StepVerifier.create(
                        userService.registerUser(registrationRequest())
                )
                .expectNext(tokenResponse)
                .verifyComplete();

        verify(outboxService).saveUserRegistered(USER_UID, EMAIL);
        verify(usersApi, never()).deleteUser(USER_UID);
    }

    @Test
    void shouldNotCompensateWhenOutboxFails() {
        when(outboxService.saveUserRegistered(USER_UID, EMAIL))
                .thenThrow(
                        new IllegalStateException("Outbox database unavailable")
                );

        StepVerifier.create(
                        userService.registerUser(registrationRequest())
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && error.getMessage().contains(
                                "Outbox database unavailable"
                        )
                )
                .verify();

        verify(usersApi, never()).deleteUser(USER_UID);
    }

    @Test
    void shouldCompensateWhenKeycloakFails() {
        when(keycloakClient.createUserInKeycloak(
                EMAIL, PASSWORD, "Test", "User", "user"
        )).thenReturn(
                Mono.error(
                        new IllegalStateException("Keycloak unavailable")
                )
        );

        StepVerifier.create(
                        userService.registerUser(registrationRequest())
                )
                .expectError()
                .verify();

        verify(usersApi).deleteUser(USER_UID);
        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldRecoverRegistrationAfterOutboxFailure() {

        // 1. Имитируем уже существующего пользователя.
        UserResponse existingUser = mock(UserResponse.class);

        when(existingUser.getId()).thenReturn(USER_UID);

        when(usersApi.getUserByEmail(EMAIL))
                .thenReturn(existingUser);

        // 2. Первая запись Outbox падает.
        // Вторая попытка завершается успешно.
        when(outboxService.saveUserRegistered(USER_UID, EMAIL))
                .thenThrow(
                        new IllegalStateException(
                                "Outbox database unavailable"
                        )
                )
                .thenReturn(UUID.randomUUID().toString());

        // 3. Первая регистрация завершается ошибкой.
        StepVerifier.create(
                        userService.registerUser(registrationRequest())
                )
                .expectError(IllegalStateException.class)
                .verify();

        // 4. Повторно выполняем восстановление.
        StepVerifier.create(
                        userService.recoverRegistration(registrationRequest())
                )
                .expectNext(tokenResponse)
                .verifyComplete();

        // 5. Person Service создал пользователя только один раз.
        verify(usersApi, times(1))
                .createUser(any(UserCreateRequest.class));

        // 6. Keycloak тоже создал пользователя только один раз.
        verify(keycloakClient, times(1))
                .createUserInKeycloak(
                        EMAIL,
                        PASSWORD,
                        "Test",
                        "User",
                        "user"
                );

        // 7. Outbox получил две попытки записи.
        verify(outboxService, times(2))
                .saveUserRegistered(USER_UID, EMAIL);

        // 8. Успешного пользователя не удаляли.
        verify(usersApi, never()).deleteUser(USER_UID);
    }
}
