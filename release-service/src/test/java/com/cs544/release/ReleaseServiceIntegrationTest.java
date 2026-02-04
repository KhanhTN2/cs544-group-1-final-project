package com.cs544.release;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.cs544.release.model.Release;
import com.cs544.release.repository.ReleaseRepository;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReleaseServiceIntegrationTest {
    static KafkaContainer kafka;
    static MongoDBContainer mongo;

    @LocalServerPort
    int port;

    @Autowired
    ReleaseRepository repository;

    String token;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> envOrDefault("KAFKA_BOOTSTRAP", bootstrapKafka()));
        registry.add("spring.data.mongodb.uri", () -> envOrDefault("MONGODB_URI", mongoUri()));
        registry.add("security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
    }

    @BeforeAll
    static void startContainers() {
        if (hasEnv("KAFKA_BOOTSTRAP") && hasEnv("MONGODB_URI")) {
            return;
        }
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));
        mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));
        kafka.start();
        mongo.start();
    }

    @AfterAll
    static void stopContainers() {
        if (kafka != null) {
            kafka.stop();
        }
        if (mongo != null) {
            mongo.stop();
        }
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
    void createsReleaseAndLists() {
        Release release = new Release("Album", "1.0");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(release)
                .post("http://localhost:" + port + "/releases")
                .then()
                .statusCode(200)
                .body("name", equalTo("Album"));

        given()
                .header("Authorization", "Bearer " + token)
                .get("http://localhost:" + port + "/releases")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    private static boolean hasEnv(String key) {
        String value = System.getenv(key);
        return value != null && !value.isBlank();
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String bootstrapKafka() {
        return kafka == null ? "" : kafka.getBootstrapServers();
    }

    private static String mongoUri() {
        return mongo == null ? "" : mongo.getReplicaSetUrl();
    }
}
