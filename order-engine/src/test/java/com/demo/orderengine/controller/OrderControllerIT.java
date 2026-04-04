package com.demo.orderengine.controller;

import com.demo.orderengine.OrderEngineIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class OrderControllerIT extends OrderEngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createOrder_returns201_withCreatedOrder() {
        var request = Map.of("customerName", "Alice", "productName", "MacBook Pro", "quantity", 1);

        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", request, Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getBody().getCustomerName()).isEqualTo("Alice");
    }

    @Test
    void createOrder_returns400_whenRequiredFieldsMissing() {
        var request = Map.of("customerName", "Alice");  // missing productName and quantity

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity("/orders", request, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_returns400_whenQuantityIsZero() {
        var request = Map.of("customerName", "Alice", "productName", "MacBook Pro", "quantity", 0);

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity("/orders", request, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOrder_returnsExistingOrder() {
        Order saved = orderRepository.save(buildOrder(OrderStatus.CREATED));

        ResponseEntity<Order> response = restTemplate.getForEntity("/orders/" + saved.getId(), Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(saved.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrder_returns404_forUnknownId() {
        ResponseEntity<ProblemDetail> response = restTemplate.getForEntity(
                "/orders/" + java.util.UUID.randomUUID(), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrdersByCustomer_returnsOrdersForCustomer() {
        orderRepository.save(buildOrderFor("Alice", OrderStatus.CREATED));
        orderRepository.save(buildOrderFor("Alice", OrderStatus.RESERVED));
        orderRepository.save(buildOrderFor("Bob", OrderStatus.CREATED));

        ResponseEntity<Order[]> response = restTemplate.getForEntity(
                "/orders?customerName=Alice", Order[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(o -> o.getCustomerName().equals("Alice"));
    }

    @Test
    void cancelOrder_returns200_andCancelledStatus() {
        Order saved = orderRepository.save(buildOrder(OrderStatus.CREATED));

        restTemplate.delete("/orders/" + saved.getId() + "/cancel");

        Order updated = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_returns404_forUnknownId() {
        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                "/orders/" + java.util.UUID.randomUUID() + "/cancel",
                org.springframework.http.HttpMethod.DELETE,
                null,
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Order buildOrder(OrderStatus status) {
        return buildOrderFor("Alice", status);
    }

    private Order buildOrderFor(String customer, OrderStatus status) {
        return Order.builder()
                .customerName(customer)
                .productName("MacBook Pro")
                .quantity(1)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
