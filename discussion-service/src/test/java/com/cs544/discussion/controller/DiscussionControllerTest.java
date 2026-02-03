package com.cs544.discussion.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.cs544.discussion.model.DiscussionMessage;
import com.cs544.discussion.repository.DiscussionRepository;
import com.cs544.discussion.service.ActivityStreamService;
import com.cs544.discussion.service.DiscussionEventProducer;

@WebFluxTest(DiscussionController.class)
@AutoConfigureWebTestClient
@Import(DiscussionControllerTest.TestSecurityConfig.class)
class DiscussionControllerTest {
    @Autowired
    WebTestClient webTestClient;

    @MockBean
    DiscussionRepository repository;

    @MockBean
    DiscussionEventProducer eventProducer;

    @MockBean
    ActivityStreamService activityStreamService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
            return http.authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
    }

    @Test
    void createMessage_requiresTaskId() throws Exception {
        webTestClient.post()
                .uri("/api/discussions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"releaseId\":\"rel-1\",\"author\":\"sam\",\"message\":\"hi\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(equalTo("taskId is required."));

        verify(repository, never()).save(any(DiscussionMessage.class));
        verify(eventProducer, never()).publishMessageCreated(any(DiscussionMessage.class));
    }

    @Test
    void createMessage_savesAndPublishesEvent() throws Exception {
        DiscussionMessage saved = new DiscussionMessage("rel-1", "task-1", null, "sam", "hello");
        saved.setId("msg-1");
        when(repository.save(any(DiscussionMessage.class))).thenReturn(saved);

        webTestClient.post()
                .uri("/api/discussions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"releaseId\":\"rel-1\",\"taskId\":\"task-1\",\"author\":\"sam\",\"message\":\"hello\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").value(equalTo("msg-1"))
                .jsonPath("$.taskId").value(equalTo("task-1"))
                .jsonPath("$.author").value(equalTo("sam"));

        verify(eventProducer).publishMessageCreated(saved);
    }

    @Test
    void listMessages_returnsMessagesByRelease() throws Exception {
        DiscussionMessage message = new DiscussionMessage("rel-1", "task-1", null, "sam", "hello");
        message.setId("msg-1");
        when(repository.findByReleaseId("rel-1")).thenReturn(List.of(message));

        webTestClient.get()
                .uri("/api/discussions/rel-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").value(equalTo("msg-1"))
                .jsonPath("$[0].releaseId").value(equalTo("rel-1"));
    }
}
