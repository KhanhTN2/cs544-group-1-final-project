package com.cs544.discussion.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements WebFilter {
    private final JwtUtil jwtUtil;
    private final AuthServiceClient authServiceClient;
    private final boolean validateEnabled;

    public JwtAuthFilter(JwtUtil jwtUtil, AuthServiceClient authServiceClient,
            @org.springframework.beans.factory.annotation.Value("${auth.validate.enabled:false}") boolean validateEnabled) {
        this.jwtUtil = jwtUtil;
        this.authServiceClient = authServiceClient;
        this.validateEnabled = validateEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            token = exchange.getRequest().getQueryParams().getFirst("token");
        }
        final String resolvedToken = token;
        if (resolvedToken != null && !resolvedToken.isBlank()) {
            Mono<Boolean> validated = validateEnabled
                    ? authServiceClient.validate(resolvedToken)
                    : Mono.just(true);
            return validated.flatMap(valid -> {
                if (!valid) {
                    exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                var claims = jwtUtil.parse(resolvedToken);
                String role = claims.get("role", String.class);
                java.util.List<GrantedAuthority> authorities;
                if (role == null || role.isBlank()) {
                    authorities = java.util.Collections.emptyList();
                } else {
                    String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities = java.util.List.of(new SimpleGrantedAuthority(normalizedRole));
                }
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), resolvedToken, authorities);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            });
        }
        return chain.filter(exchange);
    }
}
