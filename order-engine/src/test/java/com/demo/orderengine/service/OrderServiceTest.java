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
import com.demo.orderengine.statemachine.IllegalStateTransitionException;
import com.demo.orderengine.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine stateMachine;

    @Mock
    private IntegrationClient integrationClient;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, stateMachine, integrationClient, messagingTemplate);
        ReflectionTestUtils.setField(service, "topicPrefix", "/topic");
    }

    // --- createOrder ---

    @Test
    void createOrder_savesOrderWithStatusCreated() {
        CreateOrderRequest request = new CreateOrderRequest("alice", "iPhone 15", 2);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createOrder(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getCustomerName()).isEqualTo("alice");
        assertThat(saved.getProductName()).isEqualTo("iPhone 15");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void createOrder_publishesWebSocketUpdate() {
        CreateOrderRequest request = new CreateOrderRequest("alice", "iPhone 15", 1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createOrder(request);

        verify(messagingTemplate).convertAndSend(eq("/topic/orders/alice"), any(OrderStatusMessage.class));
    }

    @Test
    void createOrder_notifiesPaymentServiceAndStockService() {
        CreateOrderRequest request = new CreateOrderRequest("alice", "iPhone 15", 1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createOrder(request);

        verify(integrationClient).notifyOrderCreated(any());
        verify(integrationClient).notifyReserveStock(any());
    }

    // --- getOrder ---

    @Test
    void getOrder_returnsOrderWhenFound() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        Order result = service.getOrder(id);

        assertThat(result).isEqualTo(order);
    }

    @Test
    void getOrder_throwsOrderNotFoundExceptionWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(id))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // --- getOrdersByCustomer ---

    @Test
    void getOrdersByCustomer_delegatesToRepository() {
        List<Order> orders = List.of(anOrder(UUID.randomUUID(), OrderStatus.CREATED));
        when(orderRepository.findByCustomerNameOrderByCreatedAtDesc("alice")).thenReturn(orders);

        List<Order> result = service.getOrdersByCustomer("alice");

        assertThat(result).isEqualTo(orders);
    }

    // --- cancelOrder ---

    @Test
    void cancelOrder_fromCreated_transitionsToCancelled_noIntegrationCalls() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.CREATED, OrderEvent.USER_CANCELLED)).thenReturn(OrderStatus.CANCELLED);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelOrder(id);

        verifyNoInteractions(integrationClient);
    }

    @Test
    void cancelOrder_fromReserved_callsCancelReservation() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.RESERVED);
        Order saved = anOrder(id, OrderStatus.RELEASING_RESERVATION);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.RESERVED, OrderEvent.USER_CANCELLED)).thenReturn(OrderStatus.RELEASING_RESERVATION);
        when(orderRepository.save(any())).thenReturn(saved);

        service.cancelOrder(id);

        verify(integrationClient).notifyCancelReservation(saved);
        verify(integrationClient, never()).notifyReturnPayment(any());
    }

    @Test
    void cancelOrder_fromPaid_callsReturnPayment() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.PAID);
        Order saved = anOrder(id, OrderStatus.RETURNING_PAYMENT);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.PAID, OrderEvent.USER_CANCELLED)).thenReturn(OrderStatus.RETURNING_PAYMENT);
        when(orderRepository.save(any())).thenReturn(saved);

        service.cancelOrder(id);

        verify(integrationClient).notifyReturnPayment(saved);
        verify(integrationClient, never()).notifyCancelReservation(any());
    }

    @Test
    void cancelOrder_unknownOrder_throwsOrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelOrder(id))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_illegalTransition_rethrowsException() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.DELIVERED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(any(), any()))
                .thenThrow(new IllegalStateTransitionException(OrderStatus.DELIVERED, OrderEvent.USER_CANCELLED));

        assertThatThrownBy(() -> service.cancelOrder(id))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    // --- handleStockWebhook ---

    @Test
    void handleStockWebhook_stockReservedOnPaid_callsShipOrder() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.PAID);
        Order saved = anOrder(id, OrderStatus.READY_TO_SHIP);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.PAID, OrderEvent.STOCK_RESERVED)).thenReturn(OrderStatus.READY_TO_SHIP);
        when(orderRepository.save(any())).thenReturn(saved);

        service.handleStockWebhook(new StockWebhookRequest(id, OrderEvent.STOCK_RESERVED));

        verify(integrationClient).notifyShipOrder(saved);
    }

    @Test
    void handleStockWebhook_stockSoldOutOnCreated_noIntegrationCalls() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.CREATED, OrderEvent.STOCK_SOLD_OUT)).thenReturn(OrderStatus.CANCELLED);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleStockWebhook(new StockWebhookRequest(id, OrderEvent.STOCK_SOLD_OUT));

        verifyNoInteractions(integrationClient);
    }

    @Test
    void handleStockWebhook_unknownOrder_throwsOrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleStockWebhook(new StockWebhookRequest(id, OrderEvent.STOCK_RESERVED)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // --- handlePaymentWebhook ---

    @Test
    void handlePaymentWebhook_paymentReceivedOnCreated_noIntegrationCalls() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.CREATED, OrderEvent.PAYMENT_RECEIVED)).thenReturn(OrderStatus.PAID);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_RECEIVED));

        verifyNoInteractions(integrationClient);
    }

    @Test
    void handlePaymentWebhook_paymentReceivedOnReserved_callsShipOrder() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.RESERVED);
        Order saved = anOrder(id, OrderStatus.READY_TO_SHIP);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.RESERVED, OrderEvent.PAYMENT_RECEIVED)).thenReturn(OrderStatus.READY_TO_SHIP);
        when(orderRepository.save(any())).thenReturn(saved);

        service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_RECEIVED));

        verify(integrationClient).notifyShipOrder(saved);
    }

    @Test
    void handlePaymentWebhook_paymentTimeoutOnReserved_callsCancelReservation() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.RESERVED);
        Order saved = anOrder(id, OrderStatus.RELEASING_RESERVATION);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.RESERVED, OrderEvent.PAYMENT_TIMEOUT)).thenReturn(OrderStatus.RELEASING_RESERVATION);
        when(orderRepository.save(any())).thenReturn(saved);

        service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_TIMEOUT));

        verify(integrationClient).notifyCancelReservation(saved);
    }

    @Test
    void handlePaymentWebhook_paymentTimeoutOnCreated_noIntegrationCalls() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.CREATED, OrderEvent.PAYMENT_TIMEOUT)).thenReturn(OrderStatus.CANCELLED);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_TIMEOUT));

        verifyNoInteractions(integrationClient);
    }

    @Test
    void handlePaymentWebhook_returningPayment_callsReturnPayment() {
        UUID id = UUID.randomUUID();
        Order order = anOrder(id, OrderStatus.CREATED);
        Order saved = anOrder(id, OrderStatus.RETURNING_PAYMENT);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(stateMachine.transition(any(), any())).thenReturn(OrderStatus.RETURNING_PAYMENT);
        when(orderRepository.save(any())).thenReturn(saved);

        service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_RECEIVED));

        verify(integrationClient).notifyReturnPayment(saved);
    }

    @Test
    void handlePaymentWebhook_unknownOrder_throwsOrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handlePaymentWebhook(new PaymentWebhookRequest(id, OrderEvent.PAYMENT_RECEIVED)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // --- helpers ---

    private Order anOrder(UUID id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .customerName("alice")
                .productName("iPhone 15")
                .quantity(1)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
