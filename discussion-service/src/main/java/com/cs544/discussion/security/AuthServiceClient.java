package com.cs544.discussion.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class AuthServiceClient {
    private final WebClient webClient;

    public AuthServiceClient(@Value("${auth.validate.url:http://auth-service:8086}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<Boolean> validate(String token) {
        return webClient.get()
                .uri("/auth/validate")
                .headers(headers -> headers.setBearerAuth(token))
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .onErrorReturn(false);
    }
}
