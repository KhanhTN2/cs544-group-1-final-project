package com.cs544.release.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (validateEnabled && !authServiceClient.validate(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            var claims = jwtUtil.parse(token);
            String role = claims.get("role", String.class);
            java.util.List<GrantedAuthority> authorities = role == null
                    ? java.util.Collections.emptyList()
                    : java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), token, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
