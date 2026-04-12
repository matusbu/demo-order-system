package com.demo.stockservice.controller;

import com.demo.stockservice.service.StockService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    // --- POST /orders/reserve ---

    @Test
    void reserve_validRequest_returns200AndDelegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","productName":"iPhone 15","quantity":2}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(stockService).reserve(orderId, "iPhone 15", 2);
    }

    @Test
    void reserve_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/orders/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName":"iPhone 15","quantity":2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_blankProductName_returns400() throws Exception {
        mockMvc.perform(post("/orders/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","productName":"","quantity":2}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","productName":"iPhone 15","quantity":0}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    // --- POST /orders/{orderId}/cancel-reservation ---

    @Test
    void cancelReservation_validRequest_returns200AndDelegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders/{orderId}/cancel-reservation", orderId))
                .andExpect(status().isOk());

        verify(stockService).cancelReservation(orderId);
    }

    // --- POST /orders/{orderId}/ship ---

    @Test
    void ship_validRequest_returns200AndDelegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders/{orderId}/ship", orderId))
                .andExpect(status().isOk());

        verify(stockService).ship(orderId);
    }
}
