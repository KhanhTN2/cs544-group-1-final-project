package com.cs544.auth.service;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cs544.auth.model.User;
import com.cs544.auth.repository.UserRepository;

@Service
public class UserService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DEVELOPER");
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required.");
        }
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("role must be ADMIN or DEVELOPER.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username is already taken.");
        }
        String hash = passwordEncoder.encode(password);
        return userRepository.save(new User(username, hash, role));
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
        return user;
    }

    public User requireUser(String username) {
        return userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
