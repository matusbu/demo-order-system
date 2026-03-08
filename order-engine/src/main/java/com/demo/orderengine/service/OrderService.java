package com.demo.orderengine.service;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.dto.CreateOrderRequest;
import com.demo.orderengine.dto.PaymentWebhookRequest;
import com.demo.orderengine.dto.StockWebhookRequest;
import com.demo.orderengine.integration.IntegrationClient;
import com.demo.orderengine.repository.OrderRepository;
import com.demo.orderengine.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final IntegrationClient integrationClient;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .productName(request.productName())
                .quantity(request.quantity())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        integrationClient.notifyOrderCreated(order);
        integrationClient.notifyReserveStock(order);

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus newStatus = stateMachine.transition(order.getStatus(), OrderEvent.USER_CANCELLED);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        if (newStatus == OrderStatus.RELEASING_RESERVATION) {
            integrationClient.notifyCancelReservation(order);
        } else if (newStatus == OrderStatus.RETURNING_PAYMENT) {
            integrationClient.notifyReturnPayment(order);
        }

        return order;
    }

    @Transactional
    public Order handleStockWebhook(StockWebhookRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        OrderStatus newStatus = stateMachine.transition(order.getStatus(), request.event());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        if (newStatus == OrderStatus.READY_TO_SHIP) {
            integrationClient.notifyShipOrder(order);
        }

        return order;
    }

    @Transactional
    public Order handlePaymentWebhook(PaymentWebhookRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        OrderStatus newStatus = stateMachine.transition(order.getStatus(), request.event());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        if (newStatus == OrderStatus.READY_TO_SHIP) {
            integrationClient.notifyShipOrder(order);
        } else if (newStatus == OrderStatus.RETURNING_PAYMENT) {
            integrationClient.notifyReturnPayment(order);
        }

        return order;
    }
}
