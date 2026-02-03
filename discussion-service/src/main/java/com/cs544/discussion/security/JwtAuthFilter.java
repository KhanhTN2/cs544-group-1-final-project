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
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Mono<Boolean> validated = validateEnabled
                    ? authServiceClient.validate(token)
                    : Mono.just(true);
            return validated.flatMap(valid -> {
                if (!valid) {
                    exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                var claims = jwtUtil.parse(token);
                String role = claims.get("role", String.class);
                java.util.List<GrantedAuthority> authorities = role == null
                        ? java.util.Collections.emptyList()
                        : java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), token, authorities);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            });
        }
        return chain.filter(exchange);
    }
}
