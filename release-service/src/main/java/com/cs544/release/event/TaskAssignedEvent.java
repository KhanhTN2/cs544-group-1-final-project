package com.cs544.release.event;

public record TaskAssignedEvent(
        String developerId,
        String releaseId,
        String taskId,
        String taskTitle
) {
}
