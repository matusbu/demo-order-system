package com.demo.orderengine.controller;

import com.demo.orderengine.OrderEngineIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderEvent;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@Tag("integration")
class WebhookControllerIT extends OrderEngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // --- Stock webhook ---

    @Test
    void stockWebhook_stockReserved_fromCreated_transitionsToReserved() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.STOCK_RESERVED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void stockWebhook_stockSoldOut_fromCreated_transitionsToCancelled() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.STOCK_SOLD_OUT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void stockWebhook_reservationCancelled_fromReleasingReservation_transitionsToCancelled() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RELEASING_RESERVATION));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.RESERVATION_CANCELLED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void stockWebhook_stockReserved_fromPaid_transitionsToReadyToShip_andNotifiesShipping() {
        Order order = orderRepository.save(buildOrder(OrderStatus.PAID));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.STOCK_RESERVED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.READY_TO_SHIP);
        verify(integrationClient).notifyShipOrder(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void stockWebhook_shipmentRequested_fromReadyToShip_transitionsToShipping() {
        Order order = orderRepository.save(buildOrder(OrderStatus.READY_TO_SHIP));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.SHIPMENT_REQUESTED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void stockWebhook_deliveryConfirmed_fromShipping_transitionsToDelivered() {
        Order order = orderRepository.save(buildOrder(OrderStatus.SHIPPING));

        ResponseEntity<Order> response = postStockWebhook(order.getId(), OrderEvent.DELIVERY_CONFIRMED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void stockWebhook_returns422_forInvalidTransition() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CANCELLED));

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", order.getId(), "event", "STOCK_RESERVED"),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void stockWebhook_returns404_forUnknownOrderId() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", UUID.randomUUID(), "event", "STOCK_RESERVED"),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Payment webhook ---

    @Test
    void paymentWebhook_paymentReceived_fromCreated_transitionsToPaid() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        ResponseEntity<Order> response = postPaymentWebhook(order.getId(), OrderEvent.PAYMENT_RECEIVED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void paymentWebhook_paymentTimeout_fromCreated_transitionsToCancelled() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        ResponseEntity<Order> response = postPaymentWebhook(order.getId(), OrderEvent.PAYMENT_TIMEOUT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentWebhook_paymentReceived_fromReserved_transitionsToReadyToShip_andNotifiesShipping() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RESERVED));

        ResponseEntity<Order> response = postPaymentWebhook(order.getId(), OrderEvent.PAYMENT_RECEIVED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.READY_TO_SHIP);
        verify(integrationClient).notifyShipOrder(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void paymentWebhook_paymentTimeout_fromReserved_transitionsToReleasingReservation() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RESERVED));

        ResponseEntity<Order> response = postPaymentWebhook(order.getId(), OrderEvent.PAYMENT_TIMEOUT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.RELEASING_RESERVATION);
        verify(integrationClient).notifyCancelReservation(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void paymentWebhook_paymentReturned_fromReturningPayment_transitionsToCancelled() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RETURNING_PAYMENT));

        ResponseEntity<Order> response = postPaymentWebhook(order.getId(), OrderEvent.PAYMENT_RETURNED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentWebhook_returns422_forInvalidTransition() {
        Order order = orderRepository.save(buildOrder(OrderStatus.DELIVERED));

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/webhook/payment",
                Map.of("orderId", order.getId(), "event", "PAYMENT_RECEIVED"),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }

    private ResponseEntity<Order> postStockWebhook(UUID orderId, OrderEvent event) {
        return restTemplate.postForEntity(
                "/webhook/stock",
                Map.of("orderId", orderId, "event", event.name()),
                Order.class);
    }

    private ResponseEntity<Order> postPaymentWebhook(UUID orderId, OrderEvent event) {
        return restTemplate.postForEntity(
                "/webhook/payment",
                Map.of("orderId", orderId, "event", event.name()),
                Order.class);
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .customerName("Alice")
                .productName("MacBook Pro")
                .quantity(1)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
