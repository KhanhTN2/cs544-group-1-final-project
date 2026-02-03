package com.cs544.notification.event;

public record HotfixTaskAddedEvent(
        String developerId,
        String releaseId,
        String taskTitle
) {
}
