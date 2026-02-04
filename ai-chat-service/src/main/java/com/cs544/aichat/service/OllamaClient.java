package com.cs544.aichat.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OllamaClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ollamaBaseUrl;
    private final String model;
    private final boolean fallbackEnabled;

    public OllamaClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:llama3.2}") String model,
            @Value("${ollama.fallback-enabled:true}") boolean fallbackEnabled
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.model = model;
        this.fallbackEnabled = fallbackEnabled;
    }

    public String generateReply(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        OllamaGenerateRequest request = new OllamaGenerateRequest(model, prompt, false);
        HttpEntity<OllamaGenerateRequest> entity = new HttpEntity<>(request, headers);
        try {
            OllamaGenerateResponse response = restTemplate.postForObject(
                    ollamaBaseUrl + "/api/generate",
                    entity,
                    OllamaGenerateResponse.class
            );
            if (response == null || response.response() == null || response.response().isBlank()) {
                return "I could not generate a response right now.";
            }
            return response.response().trim();
        } catch (RestClientException ex) {
            if (fallbackEnabled) {
                return "AI reply unavailable from Ollama right now. Please retry.";
            }
            throw new IllegalStateException("Ollama request failed: " + ex.getMessage(), ex);
        }
    }

    public String generateReplyStream(String prompt, Consumer<String> chunkConsumer) {
        OllamaGenerateRequest request = new OllamaGenerateRequest(model, prompt, true);
        try {
            return restTemplate.execute(
                    ollamaBaseUrl + "/api/generate",
                    HttpMethod.POST,
                    clientHttpRequest -> writeRequest(clientHttpRequest, request),
                    clientHttpResponse -> {
                        StringBuilder fullReply = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8)
                        )) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) {
                                    continue;
                                }
                                OllamaStreamChunk chunk = objectMapper.readValue(line, OllamaStreamChunk.class);
                                if (chunk.response() != null && !chunk.response().isEmpty()) {
                                    fullReply.append(chunk.response());
                                    chunkConsumer.accept(chunk.response());
                                }
                                if (Boolean.TRUE.equals(chunk.done())) {
                                    break;
                                }
                            }
                        }
                        if (fullReply.isEmpty()) {
                            String fallback = "I could not generate a response right now.";
                            chunkConsumer.accept(fallback);
                            return fallback;
                        }
                        return fullReply.toString();
                    }
            );
        } catch (RestClientException ex) {
            if (fallbackEnabled) {
                String fallback = "AI reply unavailable from Ollama right now. Please retry.";
                chunkConsumer.accept(fallback);
                return fallback;
            }
            throw new IllegalStateException("Ollama request failed: " + ex.getMessage(), ex);
        }
    }

    private void writeRequest(ClientHttpRequest request, OllamaGenerateRequest payload) throws IOException {
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        objectMapper.writeValue(request.getBody(), payload);
    }

    public record OllamaGenerateRequest(String model, String prompt, boolean stream) {
    }

    public record OllamaGenerateResponse(String response) {
    }

    public record OllamaStreamChunk(String response, Boolean done) {
    }
}
