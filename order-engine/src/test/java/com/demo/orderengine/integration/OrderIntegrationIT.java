package com.demo.orderengine.integration;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "grpc.server.in-process-name=test-stock-service",
        "grpc.client.stock-service.address=in-process:test-stock-service"
})
@EnableWireMock({
        @ConfigureWireMock(name = "payment-service", baseUrlProperties = "integrations.payment-service.url")
})
class OrderIntegrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @TestConfiguration
    static class GrpcTestConfig {
        @Bean
        StockServiceTestDouble stockServiceTestDouble() {
            return new StockServiceTestDouble();
        }
    }

    @InjectWireMock("payment-service")
    WireMockServer paymentWireMock;

    @Autowired StockServiceTestDouble stockServiceTestDouble;
    @Autowired TestRestTemplate restTemplate;
    @Autowired OrderRepository  orderRepository;

    @BeforeEach
    void reset() {
        paymentWireMock.resetAll();
        stockServiceTestDouble.reset();
        orderRepository.deleteAll();
    }

    // ── OrderRepository queries ───────────────────────────────────────────────

    @Test
    void findByCustomerName_returnsOnlyMatchingOrdersInDescendingOrder() {
        Order older = savedOrder("alice", OrderStatus.CREATED,
                LocalDateTime.now().minusMinutes(5));
        Order newer = savedOrder("alice", OrderStatus.RESERVED,
                LocalDateTime.now());
        savedOrder("bob", OrderStatus.CREATED, LocalDateTime.now());

        var result = orderRepository.findByCustomerNameOrderByCreatedAtDesc("alice");

        assertThat(result).extracting(Order::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByCustomerName_unknownCustomer_returnsEmptyList() {
        assertThat(orderRepository.findByCustomerNameOrderByCreatedAtDesc("nobody")).isEmpty();
    }

    // ── POST /orders ──────────────────────────────────────────────────────────

    @Test
    void createOrder_persistsOrderAndNotifiesDownstreamServices() {
        paymentWireMock.stubFor(post("/orders").willReturn(ok()));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/orders",
                Map.of("customerName", "alice", "productName", "iPhone 15", "quantity", 1),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = UUID.fromString((String) response.getBody().get("id"));
        assertThat(orderRepository.findById(id)).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.CREATED);

        paymentWireMock.verify(postRequestedFor(urlEqualTo("/orders")));
        assertThat(stockServiceTestDouble.reserveRequests).hasSize(1);
        assertThat(stockServiceTestDouble.reserveRequests.get(0).getOrderId()).isEqualTo(id.toString());
        assertThat(stockServiceTestDouble.reserveRequests.get(0).getProductName()).isEqualTo("iPhone 15");
        assertThat(stockServiceTestDouble.reserveRequests.get(0).getQuantity()).isEqualTo(1);
    }

    // ── GET endpoints ─────────────────────────────────────────────────────────

    @Test
    void getOrder_returnsPersistedOrder() {
        Order order = savedOrder("alice", OrderStatus.CREATED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.getForEntity("/orders/{id}", Map.class, order.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo(order.getId().toString());
        assertThat(response.getBody().get("status")).isEqualTo("CREATED");
    }

    @Test
    void getOrder_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/orders/{id}", Map.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    @Test
    void getOrdersByCustomer_returnsMatchingOrders() {
        savedOrder("alice", OrderStatus.CREATED, LocalDateTime.now().minusMinutes(1));
        savedOrder("alice", OrderStatus.RESERVED, LocalDateTime.now());

        ResponseEntity<Object[]> response = restTemplate.getForEntity(
                "/orders?customerName=alice", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ── DELETE /orders/{id}/cancel ────────────────────────────────────────────

    @Test
    void cancelOrder_fromCreated_transitionsToCancelled_noDownstreamCalls() {
        Order order = savedOrder("alice", OrderStatus.CREATED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/orders/{id}/cancel", org.springframework.http.HttpMethod.DELETE,
                null, Map.class, order.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("CANCELLED");
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.CANCELLED);
        paymentWireMock.verify(0, anyRequestedFor(anyUrl()));
        assertNoStockServiceCalls();
    }

    @Test
    void cancelOrder_fromReserved_transitionsToReleasingReservation_callsCancelReservation() {
        Order order = savedOrder("alice", OrderStatus.RESERVED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/orders/{id}/cancel", org.springframework.http.HttpMethod.DELETE,
                null, Map.class, order.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("RELEASING_RESERVATION");
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.RELEASING_RESERVATION);
        assertThat(stockServiceTestDouble.cancelReservationOrderIds).containsExactly(order.getId().toString());
    }

    // ── POST /webhook/stock ───────────────────────────────────────────────────

    @Test
    void stockWebhook_stockReservedOnCreated_persistsReservedStatus() {
        Order order = savedOrder("alice", OrderStatus.CREATED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", order.getId(), "event", "STOCK_RESERVED"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.RESERVED);
        assertNoStockServiceCalls();
    }

    @Test
    void stockWebhook_stockReservedOnPaid_persistsReadyToShip_callsShipOrder() {
        Order order = savedOrder("alice", OrderStatus.PAID, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", order.getId(), "event", "STOCK_RESERVED"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.READY_TO_SHIP);
        assertThat(stockServiceTestDouble.shipOrderIds).containsExactly(order.getId().toString());
    }

    // ── POST /webhook/payment ─────────────────────────────────────────────────

    @Test
    void paymentWebhook_paymentReceivedOnCreated_persistsPaidStatus() {
        Order order = savedOrder("alice", OrderStatus.CREATED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/payment",
                Map.of("orderId", order.getId(), "event", "PAYMENT_RECEIVED"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void paymentWebhook_paymentTimeoutOnReserved_persistsReleasingReservation_callsCancelReservation() {
        Order order = savedOrder("alice", OrderStatus.RESERVED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/payment",
                Map.of("orderId", order.getId(), "event", "PAYMENT_TIMEOUT"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId())).isPresent()
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.RELEASING_RESERVATION);
        assertThat(stockServiceTestDouble.cancelReservationOrderIds).containsExactly(order.getId().toString());
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void stockWebhook_unknownOrderId_returns404() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", UUID.randomUUID(), "event", "STOCK_RESERVED"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    @Test
    void paymentWebhook_invalidTransition_returns422() {
        Order order = savedOrder("alice", OrderStatus.DELIVERED, LocalDateTime.now());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/payment",
                Map.of("orderId", order.getId(), "event", "PAYMENT_RECEIVED"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("status")).isEqualTo(422);
    }

    // ── Transaction behaviour ─────────────────────────────────────────────────

    @Test
    void createOrder_whenPaymentServiceFails_rollsBackDatabaseWrite() {
        paymentWireMock.stubFor(post("/orders").willReturn(serverError()));

        restTemplate.postForEntity(
                "/orders",
                Map.of("customerName", "alice", "productName", "iPhone 15", "quantity", 1),
                Map.class);

        assertThat(orderRepository.findAll()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void assertNoStockServiceCalls() {
        assertThat(stockServiceTestDouble.reserveRequests).isEmpty();
        assertThat(stockServiceTestDouble.cancelReservationOrderIds).isEmpty();
        assertThat(stockServiceTestDouble.shipOrderIds).isEmpty();
    }

    private Order savedOrder(String customerName, OrderStatus status, LocalDateTime createdAt) {
        return orderRepository.save(Order.builder()
                .customerName(customerName)
                .productName("iPhone 15")
                .quantity(1)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }
}
