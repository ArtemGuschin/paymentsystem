package com.artem.fakepaymentprovider.security;

import com.artem.fakepaymentprovider.config.ServiceTokenProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ServiceTokenFilter extends OncePerRequestFilter {

    private final ServiceTokenProperties properties;

    public ServiceTokenFilter(ServiceTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("========== FILTER START ==========");
        System.out.println("URI = " + request.getRequestURI());

        String token = request.getHeader(properties.getHeaderName());

        System.out.println("HEADER TOKEN = " + token);
        System.out.println("EXPECTED TOKEN = " + properties.getToken());

        if (token == null || !properties.getToken().equals(token)) {

            System.out.println("TOKEN INVALID");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid service token");
            return;
        }

        System.out.println("TOKEN VALID");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "payment-service",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_SERVICE")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        System.out.println(
                "AUTH BEFORE CHAIN = "
                        + SecurityContextHolder.getContext().getAuthentication()
        );

        try {
            filterChain.doFilter(request, response);
        } finally {

            System.out.println(
                    "AUTH AFTER CHAIN = "
                            + SecurityContextHolder.getContext().getAuthentication()
            );

            SecurityContextHolder.clearContext();
        }
    }
}