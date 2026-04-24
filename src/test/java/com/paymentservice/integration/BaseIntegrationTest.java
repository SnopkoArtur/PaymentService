package com.paymentservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    protected static WireMockServer wireMockServer = new WireMockServer(8111);

    static {
        mongo.start();
        kafka.start();
        wireMockServer.start();

        System.setProperty("spring.data.mongodb.uri", mongo.getReplicaSetUrl());
        System.setProperty("spring.liquibase.mongo-url", mongo.getReplicaSetUrl());
        System.setProperty("liquibase.mongodb.url", mongo.getReplicaSetUrl());
        System.setProperty("jwt.key", "very_long_secret_key_at_least_32_chars_12345");
    }

    @BeforeAll
    static void start() {
        mongo.start();
        kafka.start();
        wireMockServer.start();
        System.setProperty("liquibase.mongodb.url", mongo.getReplicaSetUrl());
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        reg.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        reg.add("EXTERNAL_API_URL", () -> "http://localhost:8111/api/v1");
        reg.add("jwt.key", () -> "very_long_secret_key_at_least_32_chars_12345");
    }
}