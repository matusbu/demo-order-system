package com.demo.orderengine.repository;

import com.demo.orderengine.OrderEngineIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class OrderRepositoryIT extends OrderEngineIntegrationTest {

    @Test
    void save_assignsUuidAndPersistsAllFields() {
        Order order = orderRepository.save(buildOrder("Alice", "MacBook Pro", 2, OrderStatus.CREATED));

        assertThat(order.getId()).isNotNull();

        Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(found.getCustomerName()).isEqualTo("Alice");
        assertThat(found.getProductName()).isEqualTo("MacBook Pro");
        assertThat(found.getQuantity()).isEqualTo(2);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        Optional<Order> result = orderRepository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByCustomerName_returnsOnlyOrdersForThatCustomer() {
        orderRepository.save(buildOrder("Alice", "MacBook Pro", 1, OrderStatus.CREATED));
        orderRepository.save(buildOrder("Alice", "iPhone 15", 1, OrderStatus.RESERVED));
        orderRepository.save(buildOrder("Bob", "iPad Pro", 1, OrderStatus.CREATED));

        List<Order> aliceOrders = orderRepository.findByCustomerNameOrderByCreatedAtDesc("Alice");

        assertThat(aliceOrders).hasSize(2);
        assertThat(aliceOrders).allMatch(o -> o.getCustomerName().equals("Alice"));
    }

    @Test
    void findByCustomerName_returnsNewestFirst() {
        LocalDateTime earlier = LocalDateTime.now().minusMinutes(5);
        LocalDateTime later = LocalDateTime.now();

        Order first = orderRepository.save(buildOrderWithTimestamp("Alice", "MacBook Pro", earlier));
        Order second = orderRepository.save(buildOrderWithTimestamp("Alice", "iPhone 15", later));

        List<Order> orders = orderRepository.findByCustomerNameOrderByCreatedAtDesc("Alice");

        assertThat(orders.get(0).getId()).isEqualTo(second.getId());
        assertThat(orders.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void findByCustomerName_returnsEmptyListForUnknownCustomer() {
        List<Order> orders = orderRepository.findByCustomerNameOrderByCreatedAtDesc("Unknown");

        assertThat(orders).isEmpty();
    }

    private Order buildOrder(String customer, String product, int qty, OrderStatus status) {
        return Order.builder()
                .customerName(customer)
                .productName(product)
                .quantity(qty)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Order buildOrderWithTimestamp(String customer, String product, LocalDateTime timestamp) {
        return Order.builder()
                .customerName(customer)
                .productName(product)
                .quantity(1)
                .status(OrderStatus.CREATED)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
