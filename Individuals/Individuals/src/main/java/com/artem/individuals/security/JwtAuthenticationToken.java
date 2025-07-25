package com.artem.individuals.security;


import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;


public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;

    public JwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        super.setAuthenticated(true); // Токен уже верифицирован
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue(); // Возвращает строковое представление токена
    }

    @Override
    public Object getPrincipal() {
        return jwt.getSubject(); // Возвращает идентификатор пользователя (subject)
    }

    public Jwt getJwt() {
        return jwt;
    }
}