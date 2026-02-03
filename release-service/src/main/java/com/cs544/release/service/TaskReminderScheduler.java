package com.cs544.release.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.model.TaskStatus;
import com.cs544.release.repository.ReleaseRepository;

@Component
public class TaskReminderScheduler {
    private final ReleaseRepository releaseRepository;
    private final ReleaseEventProducer eventProducer;
    private final long staleThresholdHours;
    private final long reminderIntervalMs;
    private final Map<String, Instant> lastReminderSent = new ConcurrentHashMap<>();

    public TaskReminderScheduler(
            ReleaseRepository releaseRepository,
            ReleaseEventProducer eventProducer,
            @Value("${release.tasks.stale-threshold-hours:24}") long staleThresholdHours,
            @Value("${release.tasks.reminder-interval-ms:3600000}") long reminderIntervalMs
    ) {
        this.releaseRepository = releaseRepository;
        this.eventProducer = eventProducer;
        this.staleThresholdHours = staleThresholdHours;
        this.reminderIntervalMs = reminderIntervalMs;
    }

    @Scheduled(fixedDelayString = "${release.tasks.reminder-interval-ms:3600000}")
    public void sendStaleTaskReminders() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(staleThresholdHours));
        Instant reminderCutoff = Instant.now().minusMillis(reminderIntervalMs);

        for (Release release : releaseRepository.findAll()) {
            if (release.getTasks() == null) {
                continue;
            }
            for (Task task : release.getTasks()) {
                if (task.getStatus() == TaskStatus.COMPLETED) {
                    continue;
                }
                Instant updatedAt = task.getUpdatedAt();
                if (updatedAt == null || updatedAt.isAfter(cutoff)) {
                    continue;
                }
                Instant lastSent = lastReminderSent.get(task.getId());
                if (lastSent != null && lastSent.isAfter(reminderCutoff)) {
                    continue;
                }
                eventProducer.publishStaleTaskReminder(release, task);
                lastReminderSent.put(task.getId(), Instant.now());
            }
        }
    }
}
