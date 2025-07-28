package com.artem.individuals.rest;

import com.artem.individuals.dto.request.AuthRequest;
import com.artem.individuals.dto.request.RegistrationRequest;
import com.artem.individuals.dto.response.TokenResponse;
import com.artem.individuals.dto.response.UserResponse;
import com.artem.individuals.client.KeycloakIntegrationClient;
import com.artem.individuals.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenResponse> register(@Valid @RequestBody RegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation does not match!!!"));
        }

        return userService.userExists(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "User already exists"));
                    }
                    return userService.registerUser(request);
                });
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody AuthRequest request) {
        return userService.loginUser(request.getEmail(), request.getPassword());
    }

    @PostMapping("/refresh-token")
    public Mono<TokenResponse> refreshToken(@Valid @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token required"));
        }
        return userService.refreshToken(refreshToken);
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(Authentication authentication) {
        return userService.getUserById(authentication.getName())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }
}
