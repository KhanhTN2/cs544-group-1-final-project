package com.cs544.release.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.release.model.Release;
import com.cs544.release.model.Task;
import com.cs544.release.security.JwtUtil;
import com.cs544.release.service.ReleaseWorkflowService;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {
    private final ReleaseWorkflowService workflowService;
    private final JwtUtil jwtUtil;
    private static final Set<String> ALLOWED_ROLES = Set.of("release-manager", "dev-1", "dev-2");

    public ReleaseController(ReleaseWorkflowService workflowService, JwtUtil jwtUtil) {
        this.workflowService = workflowService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
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
    public ResponseEntity<?> addTask(@PathVariable String id, @RequestBody TaskRequest request) {
        try {
            Task task = new Task(request.title(), request.description(), request.assigneeId(), request.orderIndex());
            return ResponseEntity.ok(workflowService.addTask(id, task));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PutMapping("/{id}/tasks/{taskId}/start")
    public ResponseEntity<?> startTask(
            @PathVariable String id,
            @PathVariable String taskId,
            @RequestBody TaskActionRequest request
    ) {
        try {
            return ResponseEntity.ok(workflowService.startTask(id, taskId, request.developerId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PutMapping("/{id}/tasks/{taskId}/complete")
    public ResponseEntity<?> completeTask(
            @PathVariable String id,
            @PathVariable String taskId,
            @RequestBody TaskActionRequest request
    ) {
        try {
            return ResponseEntity.ok(workflowService.completeTask(id, taskId, request.developerId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeRelease(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowService.completeRelease(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        if (!ALLOWED_ROLES.contains(request.username())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role."));
        }
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    public record ReleaseRequest(String name, String version) {
    }

    public record TaskRequest(String title, String description, String assigneeId, int orderIndex) {
    }

    public record TaskActionRequest(String developerId) {
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }

    public record ErrorResponse(String message) {
    }
}
