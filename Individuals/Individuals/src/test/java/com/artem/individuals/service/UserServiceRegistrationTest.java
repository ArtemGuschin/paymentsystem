
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
}
