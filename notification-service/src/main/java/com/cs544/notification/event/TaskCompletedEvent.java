package com.cs544.notification.event;

public record TaskCompletedEvent(String developerId, String releaseId, String taskId, String taskTitle) {
}
