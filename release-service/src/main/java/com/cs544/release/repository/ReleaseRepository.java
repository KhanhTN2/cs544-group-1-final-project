package com.cs544.release.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cs544.release.model.Release;

public interface ReleaseRepository extends MongoRepository<Release, String> {
}
