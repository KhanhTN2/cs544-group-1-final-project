package com.cs544.notification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class NotificationServiceIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));
    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @LocalServerPort
    int port;

    String token;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
    }

    @BeforeEach
    void setup() {
        SecretKey key = Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes());
        token = Jwts.builder()
                .setSubject("admin")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void publishesSystemError() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"service\":\"release-service\",\"message\":\"down\"}")
                .post("http://localhost:" + port + "/api/notifications/system-error")
                .then()
                .statusCode(200)
                .body("service", equalTo("release-service"));
    }
}
