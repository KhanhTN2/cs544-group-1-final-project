package com.cs544.release.event;

public record HotfixTaskAddedEvent(
        String developerId,
        String releaseId,
        String taskTitle
) {
}
