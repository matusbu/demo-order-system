package com.demo.orderengine.controller;

import com.demo.orderengine.AbstractIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest extends AbstractIntegrationTest {

    // ── POST /orders ──────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldReturn201_andNotifyPaymentAndStock() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Alice",
                                  "productName": "Widget",
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.productName").value("Widget"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(integrationClient).notifyOrderCreated(any());
        verify(integrationClient).notifyReserveStock(any());
    }

    @Test
    void createOrder_withMissingField_shouldReturn400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "customerName": "Alice" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── GET /orders/{id} ──────────────────────────────────────────────────────

    @Test
    void getOrder_shouldReturnOrder() throws Exception {
        Order order = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getOrder_unknownId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/orders/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /orders/{id}/cancel ────────────────────────────────────────────

    @Test
    void cancelOrder_fromCreated_shouldReturnCancelled_withNoExternalCalls() throws Exception {
        Order order = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(delete("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(integrationClient, never()).notifyCancelReservation(any());
        verify(integrationClient, never()).notifyReturnPayment(any());
    }

    @Test
    void cancelOrder_fromReserved_shouldReturnReleasingReservation_andCancelReservation() throws Exception {
        Order order = saveOrder(OrderStatus.RESERVED);

        mockMvc.perform(delete("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASING_RESERVATION"));

        verify(integrationClient).notifyCancelReservation(any());
        verify(integrationClient, never()).notifyReturnPayment(any());
    }

    @Test
    void cancelOrder_fromPaid_shouldReturnReturningPayment_andReturnPayment() throws Exception {
        Order order = saveOrder(OrderStatus.PAID);

        mockMvc.perform(delete("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNING_PAYMENT"));

        verify(integrationClient).notifyReturnPayment(any());
        verify(integrationClient, never()).notifyCancelReservation(any());
    }

    @Test
    void cancelOrder_fromDelivered_shouldReturn422() throws Exception {
        Order order = saveOrder(OrderStatus.DELIVERED);

        mockMvc.perform(delete("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelOrder_unknownId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/orders/00000000-0000-0000-0000-000000000000/cancel"))
                .andExpect(status().isNotFound());
    }
}
