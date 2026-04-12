package com.demo.orderengine.controller;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.service.OrderNotFoundException;
import com.demo.orderengine.service.OrderService;
import com.demo.orderengine.statemachine.IllegalStateTransitionException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // --- POST /webhook/stock ---

    @Test
    void handleStock_validRequest_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.handleStockWebhook(any())).thenReturn(anOrder(orderId, OrderStatus.RESERVED));

        mockMvc.perform(post("/webhook/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","event":"STOCK_RESERVED"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());
    }

    @Test
    void handleStock_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/webhook/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"STOCK_RESERVED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleStock_missingEvent_returns400() throws Exception {
        mockMvc.perform(post("/webhook/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleStock_unknownEventValue_returns400() throws Exception {
        mockMvc.perform(post("/webhook/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","event":"INVALID_EVENT"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    // --- POST /webhook/payment ---

    @Test
    void handlePayment_validRequest_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.handlePaymentWebhook(any())).thenReturn(anOrder(orderId, OrderStatus.PAID));

        mockMvc.perform(post("/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","event":"PAYMENT_RECEIVED"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());
    }

    @Test
    void handlePayment_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"PAYMENT_RECEIVED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePayment_unknownOrder_returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.handlePaymentWebhook(any())).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(post("/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","event":"PAYMENT_RECEIVED"}
                                """.formatted(orderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void handlePayment_invalidTransition_returns422() throws Exception {
        when(orderService.handlePaymentWebhook(any()))
                .thenThrow(new IllegalStateTransitionException(OrderStatus.DELIVERED, OrderEvent.PAYMENT_RECEIVED));

        mockMvc.perform(post("/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","event":"PAYMENT_RECEIVED"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
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
