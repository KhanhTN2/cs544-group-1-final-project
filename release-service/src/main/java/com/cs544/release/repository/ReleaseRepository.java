package com.cs544.release.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.cs544.release.model.Release;
import com.cs544.release.model.TaskStatus;

public interface ReleaseRepository extends MongoRepository<Release, String> {
    @Query("{ 'tasks.assigneeId': ?0, 'tasks.status': ?1 }")
    List<Release> findByAssigneeAndTaskStatus(String assigneeId, TaskStatus status);

    @Query("{ 'tasks.assigneeId': ?0 }")
    List<Release> findByAssignee(String assigneeId);

    @Query("{ 'tasks.id': ?0 }")
    java.util.Optional<Release> findByTaskId(String taskId);
}
