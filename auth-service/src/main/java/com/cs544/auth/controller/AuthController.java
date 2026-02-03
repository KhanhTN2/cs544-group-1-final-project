package com.cs544.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.auth.model.User;
import com.cs544.auth.security.JwtUtil;
import com.cs544.auth.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.username(), request.password(), request.role());
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(new AuthResponse(token, user.getRole()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.authenticate(request.username(), request.password());
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(new AuthResponse(token, user.getRole()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@org.springframework.web.bind.annotation.RequestHeader org.springframework.http.HttpHeaders headers) {
        String header = headers.getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(new ErrorResponse("Missing token"));
        }
        String token = header.substring(7);
        try {
            var claims = jwtUtil.parse(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            if (username == null || role == null) {
                return ResponseEntity.status(401).body(new ErrorResponse("Invalid token"));
            }
            User user = userService.requireUser(username);
            if (!user.getRole().equals(role)) {
                return ResponseEntity.status(401).body(new ErrorResponse("Invalid token"));
            }
            return ResponseEntity.ok(new AuthResponse(token, role));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid token"));
        }
    }

    public record RegisterRequest(String username, String password, String role) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(String token, String role) {
    }

    public record ErrorResponse(String message) {
    }
}
