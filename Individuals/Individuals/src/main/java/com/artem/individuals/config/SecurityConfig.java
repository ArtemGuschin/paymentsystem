package com.artem.individuals.config;

import com.artem.individuals.security.JwtAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST,
                                "/v1/auth/registration",
                                "/v1/auth/login",
                                "/v1/auth/refresh-token"
                        ).permitAll()

                        .pathMatchers(HttpMethod.GET, "/v1/auth/me").hasAnyRole("user", "admin")
                        .pathMatchers("/v1/admin/**").hasRole("admin")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                        .authenticationEntryPoint((exchange, ex) ->
                                Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage())))
                        .accessDeniedHandler((exchange, ex) ->
                                Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"))));

        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthConverter() {
        return jwt -> {
            try {
                List<String> roles = jwt.getClaim("roles");

                Collection<GrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role))
                        .collect(Collectors.toList());

                return Mono.just(new JwtAuthenticationToken(jwt, authorities));
            } catch (Exception e) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid token structure", e));
            }
        };
    }
}
