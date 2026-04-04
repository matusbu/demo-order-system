package com.demo.paymentservice.controller;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentControllerIT {

    @MockitoBean
    private OrderEngineClient orderEngineClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerOrder_returns200() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/orders", Map.of("orderId", orderId), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void registerOrder_returns400_whenOrderIdMissing() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/orders", Map.of(), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelOrder_returns200() {
        UUID orderId = UUID.randomUUID();
        restTemplate.postForEntity("/orders", Map.of("orderId", orderId), Void.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/orders/" + orderId + "/cancel", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void returnOrder_returns200_andSendsPaymentReturnedEventAfterDelay() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/orders/" + orderId + "/return", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // return delay is hardcoded to 3 seconds in PaymentTimeoutService
        verify(orderEngineClient, timeout(5000)).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RETURNED);
    }

    @Test
    void simulatePayment_returns200_andSendsPaymentReceivedEvent() {
        UUID orderId = UUID.randomUUID();
        // Register first so cancel() in processPayment() has a future to cancel (or no-op)
        restTemplate.postForEntity("/orders", Map.of("orderId", orderId), Void.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/simulate/payment",
                Map.of("orderId", orderId, "amount", "99.99"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderEngineClient, timeout(2000)).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
    }

    @Test
    void simulatePayment_returns400_whenBodyIsInvalid() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/simulate/payment", Map.of("orderId", UUID.randomUUID()), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
