package com.artem.fakepaymentprovider.config;

import com.artem.fakepaymentprovider.security.ServiceTokenFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ServiceTokenProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ServiceTokenProperties serviceTokenProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        System.out.println("========== SECURITY CONFIG LOADED ==========");

        http
                .csrf(csrf -> csrf.disable())

                .addFilterBefore(
                        new ServiceTokenFilter(serviceTokenProperties),
                        AnonymousAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/webhooks",
                                "/error"
                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

}