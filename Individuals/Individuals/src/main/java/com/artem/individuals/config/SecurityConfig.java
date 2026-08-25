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
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
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
                        .pathMatchers("/api/v1/payments/**").permitAll()

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

            Collection<GrantedAuthority> authorities =
                    new ArrayList<>();

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess != null) {

                Object rolesObject =
                        realmAccess.get("roles");

                if (rolesObject instanceof List<?> roles) {

                    for (Object role : roles) {

                        if (role instanceof String roleName) {

                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + roleName.toUpperCase()
                                    )
                            );
                        }
                    }
                }
            }

            return Mono.just(
                    new JwtAuthenticationToken(
                            jwt,
                            authorities
                    )
            );
        };
    }


}
