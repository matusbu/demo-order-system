package com.demo.orderengine.service;

import com.demo.orderengine.OrderEngineIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.dto.CreateOrderRequest;
import com.demo.orderengine.dto.PaymentWebhookRequest;
import com.demo.orderengine.dto.StockWebhookRequest;
import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.statemachine.IllegalStateTransitionException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@Tag("integration")
class OrderServiceIT extends OrderEngineIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_persistsOrderAndNotifiesIntegrations() {
        var request = new CreateOrderRequest("Alice", "MacBook Pro", 1);

        Order order = orderService.createOrder(request);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        Order persisted = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(persisted.getCustomerName()).isEqualTo("Alice");
        assertThat(persisted.getProductName()).isEqualTo("MacBook Pro");
        assertThat(persisted.getQuantity()).isEqualTo(1);

        verify(integrationClient).notifyOrderCreated(argThat(o -> o.getId().equals(order.getId())));
        verify(integrationClient).notifyReserveStock(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void getOrder_returnsPersistedOrder() {
        Order saved = orderRepository.save(buildOrder(OrderStatus.CREATED));

        Order found = orderService.getOrder(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrder_throwsWhenOrderNotFound() {
        assertThatThrownBy(() -> orderService.getOrder(java.util.UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersByCustomer_returnsAllOrdersForCustomer() {
        orderRepository.save(buildOrderFor("Alice", OrderStatus.CREATED));
        orderRepository.save(buildOrderFor("Alice", OrderStatus.RESERVED));
        orderRepository.save(buildOrderFor("Bob", OrderStatus.CREATED));

        var orders = orderService.getOrdersByCustomer("Alice");

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomerName().equals("Alice"));
    }

    @Test
    void cancelOrder_fromCreated_transitionsDirectlyToCancelled() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        Order cancelled = orderService.cancelOrder(order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_fromReserved_triggersReservationCancellation() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RESERVED));

        Order result = orderService.cancelOrder(order.getId());

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RELEASING_RESERVATION);
        verify(integrationClient).notifyCancelReservation(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void cancelOrder_fromPaid_triggersPaymentReturn() {
        Order order = orderRepository.save(buildOrder(OrderStatus.PAID));

        Order result = orderService.cancelOrder(order.getId());

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RETURNING_PAYMENT);
        verify(integrationClient).notifyReturnPayment(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void handleStockWebhook_stockReserved_fromCreated_transitionsToReserved() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CREATED));

        Order result = orderService.handleStockWebhook(
                new StockWebhookRequest(order.getId(), OrderEvent.STOCK_RESERVED));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void handleStockWebhook_stockReserved_fromPaid_transitionsToReadyToShip_andNotifiesShipping() {
        Order order = orderRepository.save(buildOrder(OrderStatus.PAID));

        Order result = orderService.handleStockWebhook(
                new StockWebhookRequest(order.getId(), OrderEvent.STOCK_RESERVED));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.READY_TO_SHIP);
        verify(integrationClient).notifyShipOrder(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void handlePaymentWebhook_paymentReceived_fromReserved_transitionsToReadyToShip_andNotifiesShipping() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RESERVED));

        Order result = orderService.handlePaymentWebhook(
                new PaymentWebhookRequest(order.getId(), OrderEvent.PAYMENT_RECEIVED));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.READY_TO_SHIP);
        verify(integrationClient).notifyShipOrder(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void handlePaymentWebhook_paymentTimeout_fromReserved_triggersReservationCancellation() {
        Order order = orderRepository.save(buildOrder(OrderStatus.RESERVED));

        Order result = orderService.handlePaymentWebhook(
                new PaymentWebhookRequest(order.getId(), OrderEvent.PAYMENT_TIMEOUT));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RELEASING_RESERVATION);
        verify(integrationClient).notifyCancelReservation(argThat(o -> o.getId().equals(order.getId())));
    }

    @Test
    void handleStockWebhook_throwsForInvalidTransition() {
        Order order = orderRepository.save(buildOrder(OrderStatus.CANCELLED));

        assertThatThrownBy(() -> orderService.handleStockWebhook(
                new StockWebhookRequest(order.getId(), OrderEvent.STOCK_RESERVED)))
                .isInstanceOf(IllegalStateTransitionException.class);

        // status must not change on failed transition
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
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
