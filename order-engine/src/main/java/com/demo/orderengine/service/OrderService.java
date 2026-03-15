package com.demo.orderengine.service;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.dto.CreateOrderRequest;
import com.demo.orderengine.dto.OrderStatusMessage;
import com.demo.orderengine.dto.PaymentWebhookRequest;
import com.demo.orderengine.dto.StockWebhookRequest;
import com.demo.orderengine.integration.IntegrationClient;
import com.demo.orderengine.repository.OrderRepository;
import com.demo.orderengine.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final IntegrationClient integrationClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${websocket.topic-prefix}")
    private String topicPrefix;

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
        publishStatusUpdate(order);

        integrationClient.notifyOrderCreated(order);
        integrationClient.notifyReserveStock(order);

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerName) {
        return orderRepository.findByCustomerNameOrderByCreatedAtDesc(customerName);
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus newStatus = applyTransition(order.getId(), order.getStatus(), OrderEvent.USER_CANCELLED);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        publishStatusUpdate(order);

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

        OrderStatus newStatus = applyTransition(order.getId(), order.getStatus(), request.event());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        publishStatusUpdate(order);

        if (newStatus == OrderStatus.READY_TO_SHIP) {
            integrationClient.notifyShipOrder(order);
        }

        return order;
    }

    @Transactional
    public Order handlePaymentWebhook(PaymentWebhookRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        OrderStatus newStatus = applyTransition(order.getId(), order.getStatus(), request.event());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        publishStatusUpdate(order);

        if (newStatus == OrderStatus.READY_TO_SHIP) {
            integrationClient.notifyShipOrder(order);
        } else if (newStatus == OrderStatus.RETURNING_PAYMENT) {
            integrationClient.notifyReturnPayment(order);
        }

        return order;
    }

    private void publishStatusUpdate(Order order) {
        String topic = topicPrefix + "/orders/" + order.getCustomerName();
        messagingTemplate.convertAndSend(topic,
                new OrderStatusMessage(order.getId(), order.getStatus(), order.getUpdatedAt()));
        log.debug("WebSocket message published for order {} on topic {} with status {}", order.getId(), topic, order.getStatus());
    }

    private OrderStatus applyTransition(UUID orderId, OrderStatus from, OrderEvent event) {
        try {
            OrderStatus to = stateMachine.transition(from, event);
            log.info("Order {} transitioned {} --[{}]--> {}", orderId, from, event, to);
            return to;
        } catch (com.demo.orderengine.statemachine.IllegalStateTransitionException e) {
            log.error("Invalid transition attempted for order {}: {} --[{}]--> not allowed", orderId, from, event);
            throw e;
        }
    }
}
