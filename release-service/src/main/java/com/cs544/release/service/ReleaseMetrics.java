package com.cs544.release.service;

import org.springframework.stereotype.Component;

import com.cs544.release.model.TaskStatus;
import com.cs544.release.repository.ReleaseRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ReleaseMetrics {
    private final MeterRegistry meterRegistry;
    private final ReleaseRepository releaseRepository;
    private final Counter tasksCompletedCounter;

    public ReleaseMetrics(MeterRegistry meterRegistry, ReleaseRepository releaseRepository) {
        this.meterRegistry = meterRegistry;
        this.releaseRepository = releaseRepository;
        this.tasksCompletedCounter = Counter.builder("tasks_completed_total")
                .description("Total tasks completed")
                .register(meterRegistry);

        Gauge.builder("active_developers_count", this, ReleaseMetrics::countActiveDevelopers)
                .description("Number of developers with an active task")
                .register(meterRegistry);
    }

    public void incrementKafkaEvent(String eventType) {
        meterRegistry.counter("kafka_events_published_total", "event", eventType).increment();
    }

    public void recordTaskCompleted() {
        tasksCompletedCounter.increment();
    }

    private double countActiveDevelopers() {
        return releaseRepository.findAll().stream()
                .filter(release -> release.getTasks() != null)
                .flatMap(release -> release.getTasks().stream())
                .filter(task -> task.getStatus() == TaskStatus.IN_PROCESS)
                .map(task -> task.getAssigneeId())
                .distinct()
                .count();
    }
}
