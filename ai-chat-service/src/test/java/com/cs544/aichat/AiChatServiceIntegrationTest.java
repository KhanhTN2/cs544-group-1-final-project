package com.cs544.aichat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AiChatServiceIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    @LocalServerPort
    int port;

    String token;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
    }

    @BeforeEach
    void setup() {
        token = given()
                .contentType("application/json")
                .body("{\"username\":\"demo\"}")
                .post("http://localhost:" + port + "/api/chat/token")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .path("token");
    }

    @Test
    void chatsWithAi() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"prompt\":\"Summarize\"}")
                .post("http://localhost:" + port + "/api/chat")
                .then()
                .statusCode(200)
                .body("reply", equalTo("AI reply to: Summarize"));
    }
}
