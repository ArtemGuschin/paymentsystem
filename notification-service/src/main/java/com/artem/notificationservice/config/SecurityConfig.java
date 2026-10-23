
package com.artem.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/notifications/*"
                        )
                        .hasRole("NOTIFICATION_INTERNAL")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/notifications/*/status"
                        )
                        .hasRole("NOTIFICATION_INTERNAL")

                        .anyRequest().denyAll()
                )

                .httpBasic(Customizer.withDefaults())

                .formLogin(AbstractHttpConfigurer::disable)

                .build();
    }
}
