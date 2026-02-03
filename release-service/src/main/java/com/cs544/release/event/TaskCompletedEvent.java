package com.cs544.release.event;

public record TaskCompletedEvent(String developerId, String releaseId, String taskId, String taskTitle) {
}
