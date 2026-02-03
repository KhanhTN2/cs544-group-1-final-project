package com.cs544.discussion;

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
class DiscussionServiceIntegrationTest {
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
                .setSubject("dev-1")
                .claim("role", "DEVELOPER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void createsMessageAndLists() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"releaseId\":\"rel-1\",\"author\":\"sam\",\"message\":\"hello\"}")
                .post("http://localhost:" + port + "/api/discussions")
                .then()
                .statusCode(200)
                .body("author", equalTo("sam"));

        given()
                .header("Authorization", "Bearer " + token)
                .get("http://localhost:" + port + "/api/discussions/rel-1")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }
}
