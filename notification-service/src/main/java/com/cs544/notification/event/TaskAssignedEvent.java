package com.cs544.notification.event;

public record TaskAssignedEvent(
        String developerId,
        String releaseId,
        String taskId,
        String taskTitle
) {
}
