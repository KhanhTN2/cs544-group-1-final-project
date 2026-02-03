package com.cs544.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cs544.auth.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findFirstByUsername(String username);
    boolean existsByUsername(String username);
}
