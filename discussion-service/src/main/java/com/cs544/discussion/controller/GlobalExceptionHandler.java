package com.cs544.discussion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cs544.discussion.service.SystemErrorPublisher;

import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final SystemErrorPublisher errorPublisher;

    public GlobalExceptionHandler(SystemErrorPublisher errorPublisher) {
        this.errorPublisher = errorPublisher;
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex) {
        errorPublisher.publish(ex.getMessage() == null ? "Unhandled error" : ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error")));
    }

    public record ErrorResponse(String message) {
    }
}
