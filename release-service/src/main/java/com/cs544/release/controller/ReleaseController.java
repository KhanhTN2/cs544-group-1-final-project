package com.cs544.release.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.service.ReleaseWorkflowService;

@RestController
@RequestMapping({"/api/releases", "/releases"})
public class ReleaseController {
    private final ReleaseWorkflowService workflowService;

    public ReleaseController(ReleaseWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Release> createRelease(@RequestBody ReleaseRequest request) {
        return ResponseEntity.ok(workflowService.createRelease(request.name(), request.version()));
    }

    @GetMapping
    public ResponseEntity<List<Release>> listReleases() {
        return ResponseEntity.ok(workflowService.listReleases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRelease(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowService.getRelease(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addTask(@PathVariable String id, @RequestBody TaskRequest request) {
        try {
            Task task = new Task(request.title(), request.description(), request.assigneeId(), request.orderIndex());
            return ResponseEntity.ok(workflowService.addTask(id, task));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeRelease(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowService.completeRelease(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    public record ReleaseRequest(String name, String version) {
    }

    public record TaskRequest(String title, String description, String assigneeId, int orderIndex) {
    }

    public record ErrorResponse(String message) {
    }
}
