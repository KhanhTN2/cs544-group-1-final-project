package com.cs544.discussion.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cs544.discussion.model.DiscussionMessage;
import com.cs544.discussion.repository.DiscussionRepository;
import com.cs544.discussion.security.JwtUtil;
import com.cs544.discussion.service.DiscussionEventProducer;

@WebMvcTest(DiscussionController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiscussionControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DiscussionRepository repository;

    @MockBean
    DiscussionEventProducer eventProducer;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    void createMessage_requiresTaskId() throws Exception {
        mockMvc.perform(post("/api/discussions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"releaseId\":\"rel-1\",\"author\":\"sam\",\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("taskId is required.")));

        verify(repository, never()).save(any(DiscussionMessage.class));
        verify(eventProducer, never()).publishMessageCreated(any(DiscussionMessage.class));
    }

    @Test
    void createMessage_savesAndPublishesEvent() throws Exception {
        DiscussionMessage saved = new DiscussionMessage("rel-1", "task-1", null, "sam", "hello");
        saved.setId("msg-1");
        when(repository.save(any(DiscussionMessage.class))).thenReturn(saved);

        mockMvc.perform(post("/api/discussions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"releaseId\":\"rel-1\",\"taskId\":\"task-1\",\"author\":\"sam\",\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("msg-1")))
                .andExpect(jsonPath("$.taskId", equalTo("task-1")))
                .andExpect(jsonPath("$.author", equalTo("sam")));

        verify(eventProducer).publishMessageCreated(saved);
    }

    @Test
    void listMessages_returnsMessagesByRelease() throws Exception {
        DiscussionMessage message = new DiscussionMessage("rel-1", "task-1", null, "sam", "hello");
        message.setId("msg-1");
        when(repository.findByReleaseId("rel-1")).thenReturn(List.of(message));

        mockMvc.perform(get("/api/discussions/rel-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo("msg-1")))
                .andExpect(jsonPath("$[0].releaseId", equalTo("rel-1")));
    }
}
