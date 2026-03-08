package com.demo.orderengine.controller;

import com.demo.orderengine.AbstractIntegrationTest;
import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebhookControllerTest extends AbstractIntegrationTest {

    // ── POST /webhook/stock ───────────────────────────────────────────────────

    @Test
    void stockWebhook_stockReservedOnCreated_shouldTransitionToReserved() throws Exception {
        Order order = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "STOCK_RESERVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));

        verify(integrationClient, never()).notifyShipOrder(any());
    }

    @Test
    void stockWebhook_stockReservedOnPaid_shouldTransitionToReadyToShip_andShipOrder() throws Exception {
        Order order = saveOrder(OrderStatus.PAID);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "STOCK_RESERVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_SHIP"));

        verify(integrationClient).notifyShipOrder(any());
    }

    @Test
    void stockWebhook_stockSoldOutOnCreated_shouldTransitionToCancelled() throws Exception {
        Order order = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "STOCK_SOLD_OUT" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void stockWebhook_reservationCancelledOnReleasingReservation_shouldTransitionToCancelled() throws Exception {
        Order order = saveOrder(OrderStatus.RELEASING_RESERVATION);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "RESERVATION_CANCELLED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void stockWebhook_deliveryConfirmedOnShipping_shouldTransitionToDelivered() throws Exception {
        Order order = saveOrder(OrderStatus.SHIPPING);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "DELIVERY_CONFIRMED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void stockWebhook_invalidTransition_shouldReturn422() throws Exception {
        Order order = saveOrder(OrderStatus.DELIVERED);

        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "STOCK_RESERVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void stockWebhook_unknownOrderId_shouldReturn404() throws Exception {
        mockMvc.perform(post("/webhook/stock")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "00000000-0000-0000-0000-000000000000", "event": "STOCK_RESERVED" }
                                """))
                .andExpect(status().isNotFound());
    }

    // ── POST /webhook/payment ─────────────────────────────────────────────────

    @Test
    void paymentWebhook_paymentReceivedOnCreated_shouldTransitionToPaid() throws Exception {
        Order order = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(post("/webhook/payment")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "PAYMENT_RECEIVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void paymentWebhook_paymentReceivedOnReserved_shouldTransitionToReadyToShip_andShipOrder() throws Exception {
        Order order = saveOrder(OrderStatus.RESERVED);

        mockMvc.perform(post("/webhook/payment")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "PAYMENT_RECEIVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_SHIP"));

        verify(integrationClient).notifyShipOrder(any());
    }

    @Test
    void paymentWebhook_paymentTimeoutOnReserved_shouldTransitionToReleasingReservation() throws Exception {
        Order order = saveOrder(OrderStatus.RESERVED);

        mockMvc.perform(post("/webhook/payment")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "PAYMENT_TIMEOUT" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASING_RESERVATION"));
    }

    @Test
    void paymentWebhook_paymentReturnedOnReturningPayment_shouldTransitionToCancelled() throws Exception {
        Order order = saveOrder(OrderStatus.RETURNING_PAYMENT);

        mockMvc.perform(post("/webhook/payment")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "PAYMENT_RETURNED" }
                                """.formatted(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void paymentWebhook_invalidTransition_shouldReturn422() throws Exception {
        Order order = saveOrder(OrderStatus.DELIVERED);

        mockMvc.perform(post("/webhook/payment")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "orderId": "%s", "event": "PAYMENT_RECEIVED" }
                                """.formatted(order.getId())))
                .andExpect(status().isUnprocessableEntity());
    }
}
