package com.cs544.notification.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cs544.notification.model.NotificationLog;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
}
