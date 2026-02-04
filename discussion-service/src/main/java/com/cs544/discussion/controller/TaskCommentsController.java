package com.cs544.discussion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.discussion.model.DiscussionMessage;
import com.cs544.discussion.repository.DiscussionRepository;
import com.cs544.discussion.service.ActivityStreamService;
import com.cs544.discussion.service.DiscussionEventProducer;

import reactor.core.publisher.Mono;

@RestController
public class TaskCommentsController {
    private final DiscussionRepository repository;
    private final DiscussionEventProducer eventProducer;
    private final ActivityStreamService activityStreamService;

    public TaskCommentsController(
            DiscussionRepository repository,
            DiscussionEventProducer eventProducer,
            ActivityStreamService activityStreamService
    ) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.activityStreamService = activityStreamService;
    }

    @PostMapping("/tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public Mono<ResponseEntity<?>> createTaskComment(
            @PathVariable String taskId,
            @RequestBody TaskCommentRequest request
    ) {
        return Mono.fromSupplier(() -> {
            if (request.releaseId() == null || request.releaseId().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("releaseId is required."));
            }
            if (request.author() == null || request.author().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("author is required."));
            }
            if (request.message() == null || request.message().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("message is required."));
            }
            DiscussionMessage message = repository.save(new DiscussionMessage(
                    request.releaseId(),
                    taskId,
                    null,
                    request.author(),
                    request.message()
            ));
            eventProducer.publishMessageCreated(message);
            activityStreamService.emitDiscussion(message);
            return ResponseEntity.ok(message);
        });
    }

    @PostMapping("/comments/{commentId}/reply")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public Mono<ResponseEntity<?>> replyToComment(
            @PathVariable String commentId,
            @RequestBody ReplyRequest request
    ) {
        return Mono.fromSupplier(() -> {
            if (request.releaseId() == null || request.releaseId().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("releaseId is required."));
            }
            if (request.taskId() == null || request.taskId().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("taskId is required."));
            }
            if (request.author() == null || request.author().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("author is required."));
            }
            if (request.message() == null || request.message().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("message is required."));
            }
            DiscussionMessage message = repository.save(new DiscussionMessage(
                    request.releaseId(),
                    request.taskId(),
                    commentId,
                    request.author(),
                    request.message()
            ));
            eventProducer.publishMessageCreated(message);
            activityStreamService.emitDiscussion(message);
            return ResponseEntity.ok(message);
        });
    }

    @GetMapping("/tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public Mono<ResponseEntity<List<DiscussionMessage>>> listTaskComments(@PathVariable String taskId) {
        return Mono.fromSupplier(() -> ResponseEntity.ok(repository.findByTaskId(taskId)));
    }

    public record TaskCommentRequest(String releaseId, String author, String message) {
    }

    public record ReplyRequest(String releaseId, String taskId, String author, String message) {
    }

    public record ErrorResponse(String message) {
    }
}
