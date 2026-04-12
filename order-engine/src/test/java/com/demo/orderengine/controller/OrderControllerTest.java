package com.demo.orderengine.controller;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.domain.OrderStatus;
import com.demo.orderengine.service.OrderNotFoundException;
import com.demo.orderengine.service.OrderService;
import com.demo.orderengine.statemachine.IllegalStateTransitionException;
import com.demo.orderengine.domain.OrderEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // --- POST /orders ---

    @Test
    void createOrder_validRequest_returns201() throws Exception {
        Order created = anOrder(UUID.randomUUID(), OrderStatus.CREATED);
        when(orderService.createOrder(any())).thenReturn(created);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"iPhone 15","quantity":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createOrder_missingCustomerName_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName":"iPhone 15","quantity":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_blankProductName_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"","quantity":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_missingQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"iPhone 15"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"iPhone 15","quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_invalidJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"iPhone 15","quantity":"not-a-number"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_unexpectedServiceError_returns500() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"alice","productName":"iPhone 15","quantity":1}
                                """))
                .andExpect(status().isInternalServerError());
    }

    // --- GET /orders/{id} ---

    @Test
    void getOrder_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenReturn(anOrder(id, OrderStatus.CREATED));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getOrder_unknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    // --- GET /orders?customerName ---

    @Test
    void getOrdersByCustomer_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrdersByCustomer("alice")).thenReturn(List.of(anOrder(id, OrderStatus.CREATED)));

        mockMvc.perform(get("/orders").param("customerName", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    // --- DELETE /orders/{id}/cancel ---

    @Test
    void cancelOrder_validId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.cancelOrder(id)).thenReturn(anOrder(id, OrderStatus.CANCELLED));

        mockMvc.perform(delete("/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_unknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.cancelOrder(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(delete("/orders/{id}/cancel", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_invalidTransition_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.cancelOrder(id))
                .thenThrow(new IllegalStateTransitionException(OrderStatus.DELIVERED, OrderEvent.USER_CANCELLED));

        mockMvc.perform(delete("/orders/{id}/cancel", id))
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
