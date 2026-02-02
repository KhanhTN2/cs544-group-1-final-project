package com.cs544.release;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReleaseServiceIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));
    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @LocalServerPort
    int port;

    @Autowired
    ReleaseRepository repository;

    String token;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
    }

    @BeforeEach
    void setup() {
        token = given()
                .contentType("application/json")
                .body("{\"username\":\"demo\"}")
                .post("http://localhost:" + port + "/api/releases/token")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .path("token");
    }

    @Test
    void createsReleaseAndLists() {
        Release release = new Release("Album", "1.0");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(release)
                .post("http://localhost:" + port + "/api/releases")
                .then()
                .statusCode(200)
                .body("name", equalTo("Album"));

        given()
                .header("Authorization", "Bearer " + token)
                .get("http://localhost:" + port + "/api/releases")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }
}
