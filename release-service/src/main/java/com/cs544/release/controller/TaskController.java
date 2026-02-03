package com.cs544.release.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.service.ReleaseWorkflowService;

@RestController
@RequestMapping({"/api/tasks", "/tasks"})
public class TaskController {
    private final ReleaseWorkflowService workflowService;

    public TaskController(ReleaseWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<List<TaskResponse>> myTasks(Authentication authentication) {
        String developerId = authentication.getName();
        List<TaskResponse> tasks = workflowService.listTasksForDeveloper(developerId).stream()
                .map(task -> new TaskResponse(task.releaseId(), task.task()))
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<?> startTask(@PathVariable String id, Authentication authentication) {
        try {
            String developerId = authentication.getName();
            Release release = workflowService.startTaskByTaskId(id, developerId);
            Task task = release.getTasks().stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null);
            return ResponseEntity.ok(new TaskResponse(release.getId(), task));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<?> completeTask(@PathVariable String id, Authentication authentication) {
        try {
            String developerId = authentication.getName();
            Release release = workflowService.completeTaskByTaskId(id, developerId);
            Task task = release.getTasks().stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null);
            return ResponseEntity.ok(new TaskResponse(release.getId(), task));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    public record TaskResponse(String releaseId, Task task) {
    }

    public record ErrorResponse(String message) {
    }
}
