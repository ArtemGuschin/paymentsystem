package com.artem.paymentservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final IncomingSecurityProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder
    ) {

        return new AuthenticationProvider() {

            @Override
            public org.springframework.security.core.Authentication authenticate(
                    org.springframework.security.core.Authentication authentication
            ) {

                String username = authentication.getName();

                String password =
                        authentication.getCredentials().toString();

                if (!properties.getUsername().equals(username)) {
                    throw new BadCredentialsException("Invalid username");
                }

                if (!passwordEncoder.matches(
                        password,
                        properties.getPasswordHash()
                )) {
                    throw new BadCredentialsException("Invalid password");
                }

                return new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_API")
                );
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class
                        .isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http
    ) throws Exception {

        http.csrf(
                org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable
        );

        http.sessionManagement(session ->
                session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                )
        );

        http.authenticationProvider(authenticationProvider(passwordEncoder()));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.httpBasic(
                org.springframework.security.config.Customizer.withDefaults()
        );

        return http.build();
    }
}