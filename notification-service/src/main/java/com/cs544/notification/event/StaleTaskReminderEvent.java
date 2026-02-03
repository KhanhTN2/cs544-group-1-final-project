package com.cs544.notification.event;

import java.time.Instant;

public record StaleTaskReminderEvent(
        String developerId,
        String releaseId,
        String taskId,
        String taskTitle,
        Instant lastUpdatedAt
) {
}
