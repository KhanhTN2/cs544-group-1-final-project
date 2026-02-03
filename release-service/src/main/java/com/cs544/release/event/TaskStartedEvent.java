package com.cs544.release.event;

public record TaskStartedEvent(String developerId, String releaseId, String taskId, String taskTitle) {
}
