package com.cs544.release.event;

import java.time.Instant;

public record StaleTaskReminderEvent(
        String developerId,
        String releaseId,
        String taskId,
        String taskTitle,
        Instant lastUpdatedAt
) {
}
