package com.cs544.release.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.model.TaskStatus;
import com.cs544.release.repository.ReleaseRepository;

@ExtendWith(MockitoExtension.class)
class ReleaseWorkflowServiceTest {
    @Mock
    ReleaseRepository releaseRepository;

    @Mock
    ReleaseEventProducer eventProducer;

    @InjectMocks
    ReleaseWorkflowService service;

    Release release;

    @BeforeEach
    void setUp() {
        release = new Release("Apollo", "2.1");
        release.setId("rel-1");
        release.setTasks(new ArrayList<>());
    }

    @Test
    void createRelease_resetsCompletionState_andPublishesEvent() {
        when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Release created = service.createRelease("Apollo", "2.1");

        assertThat(created.isCompleted()).isFalse();
        assertThat(created.getCompletedAt()).isNull();
        assertThat(created.getLastCompletedAt()).isNull();
        verify(eventProducer).publishReleaseCreated(created);
    }

    @Test
    void addTask_reopensCompletedRelease_andPublishesHotfixEvent() {
        release.setCompleted(true);
        release.setCompletedAt(Instant.parse("2025-01-01T00:00:00Z"));
        when(releaseRepository.findById("rel-1")).thenReturn(Optional.of(release));
        when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task task = new Task("Hotfix", "Urgent fix", "dev-1", 1);
        Release updated = service.addTask("rel-1", task);

        assertThat(updated.isCompleted()).isFalse();
        assertThat(updated.getCompletedAt()).isNull();
        verify(eventProducer).publishHotfixTaskAdded(updated, task);
        assertThat(updated.getTasks()).hasSize(1);
        assertThat(updated.getTasks().get(0).getTitle()).isEqualTo("Hotfix");
    }

    @Test
    void startTask_rejectsWhenPreviousTaskNotCompleted() {
        Task task1 = task("t1", "dev-1", 1, TaskStatus.TODO);
        Task task2 = task("t2", "dev-1", 2, TaskStatus.TODO);
        release.setTasks(List.of(task1, task2));
        when(releaseRepository.findById("rel-1")).thenReturn(Optional.of(release));
        when(releaseRepository.findByAssigneeAndTaskStatus("dev-1", TaskStatus.IN_PROCESS))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.startTask("rel-1", "t2", "dev-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Previous task must be completed before starting this one.");
        verify(releaseRepository, never()).save(any(Release.class));
    }

    @Test
    void startTask_rejectsWhenDeveloperAlreadyHasActiveTask() {
        Task task1 = task("t1", "dev-1", 1, TaskStatus.COMPLETED);
        Task task2 = task("t2", "dev-1", 2, TaskStatus.TODO);
        release.setTasks(List.of(task1, task2));

        Release otherRelease = new Release("Other", "1.0");
        otherRelease.setId("rel-2");
        Task active = task("t3", "dev-1", 1, TaskStatus.IN_PROCESS);
        otherRelease.setTasks(List.of(active));

        when(releaseRepository.findById("rel-1")).thenReturn(Optional.of(release));
        when(releaseRepository.findByAssigneeAndTaskStatus("dev-1", TaskStatus.IN_PROCESS))
                .thenReturn(List.of(otherRelease));

        assertThatThrownBy(() -> service.startTask("rel-1", "t2", "dev-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Developer already has a task in progress.");
        verify(releaseRepository, never()).save(any(Release.class));
    }

    @Test
    void completeRelease_rejectsWhenAnyTaskIncomplete() {
        Task task1 = task("t1", "dev-1", 1, TaskStatus.COMPLETED);
        Task task2 = task("t2", "dev-2", 2, TaskStatus.TODO);
        release.setTasks(List.of(task1, task2));
        when(releaseRepository.findById("rel-1")).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.completeRelease("rel-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All tasks must be completed before finishing the release.");
    }

    @Test
    void completeRelease_marksCompletedAndSetsTimestamps() {
        Task task1 = task("t1", "dev-1", 1, TaskStatus.COMPLETED);
        Task task2 = task("t2", "dev-2", 2, TaskStatus.COMPLETED);
        release.setTasks(List.of(task1, task2));
        when(releaseRepository.findById("rel-1")).thenReturn(Optional.of(release));
        when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Release updated = service.completeRelease("rel-1");

        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getLastCompletedAt()).isNotNull();
        assertThat(updated.getCompletedAt()).isEqualTo(updated.getLastCompletedAt());
        ArgumentCaptor<Release> saved = ArgumentCaptor.forClass(Release.class);
        verify(releaseRepository).save(saved.capture());
        assertThat(saved.getValue().isCompleted()).isTrue();
    }

    private Task task(String id, String assigneeId, int orderIndex, TaskStatus status) {
        Task task = new Task("Task " + id, "", assigneeId, orderIndex);
        task.setId(id);
        task.setStatus(status);
        return task;
    }
}
