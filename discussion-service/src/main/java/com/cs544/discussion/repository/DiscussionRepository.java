package com.cs544.discussion.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cs544.discussion.model.DiscussionMessage;

public interface DiscussionRepository extends MongoRepository<DiscussionMessage, String> {
    List<DiscussionMessage> findByReleaseId(String releaseId);
    List<DiscussionMessage> findByTaskId(String taskId);
}
