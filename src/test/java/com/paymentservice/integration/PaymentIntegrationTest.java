package com.paymentservice.integration;

import com.paymentservice.dao.PaymentRepository;
import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.entity.Payment;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private PaymentRepository paymentRepository;

    private String adminToken;
    private String userToken;
    private final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wireMockServer.resetAll();

        var key = Keys.hmacShaKeyFor("very_long_secret_key_at_least_32_chars_12345".getBytes(StandardCharsets.UTF_8));

        adminToken = "Bearer " + Jwts.builder().claim("userId", 999L).claim("role", "ADMIN").signWith(key).compact();
        userToken = "Bearer " + Jwts.builder().claim("userId", TEST_USER_ID).claim("role", "USER").signWith(key).compact();
    }

    @Test
    void fullPaymentFlow_Success() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/payment"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json").withBody("2")));

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(101L);
        request.setUserId(TEST_USER_ID);
        request.setPaymentAmount(new BigDecimal("150.00"));

        webTestClient.post().uri("/api/v1/payments")
                .header("Authorization", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");

        assertEquals(1, paymentRepository.count());
    }

    @Test
    void fullPaymentFlow_Failed() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/payment"))
                .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("1")));

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(102L);
        request.setUserId(TEST_USER_ID);
        request.setPaymentAmount(new BigDecimal("50.00"));

        webTestClient.post().uri("/api/v1/payments")
                .header("Authorization", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED");
    }

    @Test
    void createPayment_ValidationError() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentAmount(new BigDecimal("-10.00"));

        webTestClient.post().uri("/api/v1/payments")
                .header("Authorization", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getTotal_ShouldCalculateCorrectly() {
        savePayment(11L, 1L, "100.22", "SUCCESS");
        savePayment(12L, 2L, "200.33", "SUCCESS");
        savePayment(13L, 3L, "50.22", "SUCCESS");
        String from = LocalDateTime.now().minusDays(1).toString();
        String to = LocalDateTime.now().plusDays(1).toString();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/payments/total")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header("Authorization", userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .isEqualTo(new BigDecimal("100.22"));
    }

    @Test
    void getAdminTotal_ShouldCalculateAllUsers() {
        savePayment(1001L, 1L, "100.1", "SUCCESS");
        savePayment(1002L, 2L, "200", "SUCCESS");
        savePayment(1003L, 3L, "50", "SUCCESS");

        String from = LocalDateTime.now().minusDays(1).toString();
        String to = LocalDateTime.now().plusDays(1).toString();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/payments/admin/total")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header("Authorization", adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .isEqualTo(new BigDecimal("350.1"));
    }

    private void savePayment(Long orderId,Long userId, String amount, String status) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setUserId(userId);
        p.setPaymentAmount(new BigDecimal(amount));
        p.setStatus(status);
        p.setTimestamp(LocalDateTime.now());
        paymentRepository.save(p);
    }


}