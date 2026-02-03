package com.cs544.discussion.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.discussion.service.ActivityStreamService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping({"/activity", "/api/activity"})
public class ActivityController {
    private final ActivityStreamService activityStreamService;

    public ActivityController(ActivityStreamService activityStreamService) {
        this.activityStreamService = activityStreamService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public Flux<ServerSentEvent<ActivityStreamService.ActivityEvent>> stream() {
        return activityStreamService.stream()
                .map(event -> ServerSentEvent.builder(event)
                        .event(event.type())
                        .build());
    }
}
