package com.cs544.release.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.model.TaskStatus;
import com.cs544.release.repository.ReleaseRepository;

@Service
public class ReleaseWorkflowService {
    private final ReleaseRepository releaseRepository;
    private final ReleaseEventProducer eventProducer;
    private final ReleaseMetrics metrics;

    public ReleaseWorkflowService(
            ReleaseRepository releaseRepository,
            ReleaseEventProducer eventProducer,
            ReleaseMetrics metrics
    ) {
        this.releaseRepository = releaseRepository;
        this.eventProducer = eventProducer;
        this.metrics = metrics;
    }

    public Release getRelease(String id) {
        Release release = releaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Release not found."));
        if (release.getTasks() == null) {
            release.setTasks(new java.util.ArrayList<>());
        }
        return release;
    }

    public List<Release> listReleases() {
        return releaseRepository.findAll();
    }

    public Release createRelease(String name, String version) {
        Release release = new Release(name, version);
        release.setCompleted(false);
        release.setCompletedAt(null);
        release.setLastCompletedAt(null);
        Release saved = releaseRepository.save(release);
        eventProducer.publishReleaseCreated(saved);
        return saved;
    }

    public Release addTask(String releaseId, Task task) {
        Release release = getRelease(releaseId);

        if (task.getOrderIndex() <= 0) {
            throw new IllegalArgumentException("orderIndex must be a positive integer.");
        }

        boolean duplicateIndex = release.getTasks().stream()
                .anyMatch(existing -> existing.getOrderIndex() == task.getOrderIndex());
        if (duplicateIndex) {
            throw new IllegalArgumentException("orderIndex is already used by another task.");
        }

        release.getTasks().add(task);
        release.getTasks().sort(Comparator.comparingInt(Task::getOrderIndex));
        eventProducer.publishTaskAssigned(release, task);

        if (release.isCompleted()) {
            release.setCompleted(false);
            release.setCompletedAt(null);
            eventProducer.publishHotfixTaskAdded(release, task);
        }

        return releaseRepository.save(release);
    }

    public Release startTask(String releaseId, String taskId, String developerId) {
        Release release = getRelease(releaseId);
        Task task = findTask(release, taskId);

        if (!task.getAssigneeId().equals(developerId)) {
            throw new IllegalArgumentException("Only the assigned developer can start this task.");
        }

        if (task.getStatus() != TaskStatus.TODO) {
            throw new IllegalArgumentException("Task must be TODO before it can be started.");
        }

        ensurePreviousTaskCompleted(release, task);
        ensureDeveloperHasNoActiveTask(developerId);

        task.setStatus(TaskStatus.IN_PROCESS);
        eventProducer.publishTaskStarted(release, task);
        return releaseRepository.save(release);
    }

    public Release completeTask(String releaseId, String taskId, String developerId) {
        Release release = getRelease(releaseId);
        Task task = findTask(release, taskId);

        if (!task.getAssigneeId().equals(developerId)) {
            throw new IllegalArgumentException("Only the assigned developer can complete this task.");
        }

        if (task.getStatus() != TaskStatus.IN_PROCESS) {
            throw new IllegalArgumentException("Task must be IN_PROCESS before it can be completed.");
        }

        task.setStatus(TaskStatus.COMPLETED);
        eventProducer.publishTaskCompleted(release, task);
        metrics.recordTaskCompleted();
        return releaseRepository.save(release);
    }

    public Release startTaskByTaskId(String taskId, String developerId) {
        Release release = releaseRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
        return startTask(release.getId(), taskId, developerId);
    }

    public Release completeTaskByTaskId(String taskId, String developerId) {
        Release release = releaseRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
        return completeTask(release.getId(), taskId, developerId);
    }

    public List<TaskWithRelease> listTasksForDeveloper(String developerId) {
        return releaseRepository.findByAssignee(developerId).stream()
                .filter(release -> release.getTasks() != null)
                .flatMap(release -> release.getTasks().stream()
                        .filter(task -> developerId.equals(task.getAssigneeId()))
                        .map(task -> new TaskWithRelease(release.getId(), task)))
                .toList();
    }

    public record TaskWithRelease(String releaseId, Task task) {
    }

    public Release completeRelease(String releaseId) {
        Release release = getRelease(releaseId);

        if (release.isCompleted()) {
            throw new IllegalArgumentException("Release is already completed.");
        }

        boolean allCompleted = release.getTasks().stream()
                .allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
        if (!allCompleted) {
            throw new IllegalArgumentException("All tasks must be completed before finishing the release.");
        }

        release.setCompleted(true);
        Instant now = Instant.now();
        release.setCompletedAt(now);
        release.setLastCompletedAt(now);
        return releaseRepository.save(release);
    }

    private Task findTask(Release release, String taskId) {
        return release.getTasks().stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private void ensurePreviousTaskCompleted(Release release, Task task) {
        int previousIndex = task.getOrderIndex() - 1;
        if (previousIndex <= 0) {
            return;
        }
        Optional<Task> previousTask = release.getTasks().stream()
                .filter(existing -> existing.getOrderIndex() == previousIndex)
                .findFirst();
        if (previousTask.isEmpty()) {
            throw new IllegalArgumentException("Previous task is missing; cannot start this task.");
        }
        if (previousTask.get().getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Previous task must be completed before starting this one.");
        }
    }

    private void ensureDeveloperHasNoActiveTask(String developerId) {
        boolean hasActiveTask = releaseRepository.findByAssigneeAndTaskStatus(developerId, TaskStatus.IN_PROCESS)
                .stream()
                .flatMap(release -> release.getTasks().stream())
                .anyMatch(task -> developerId.equals(task.getAssigneeId()) && task.getStatus() == TaskStatus.IN_PROCESS);
        if (hasActiveTask) {
            throw new IllegalArgumentException("Developer already has a task in progress.");
        }
    }
}
