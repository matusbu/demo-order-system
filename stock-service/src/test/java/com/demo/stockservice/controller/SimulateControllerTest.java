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
@WebMvcTest(SimulateController.class)
class SimulateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void stockAvailable_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/simulate/stock-available")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(stockService).simulateStockAvailable(orderId);
    }

    @Test
    void stockAvailable_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/simulate/stock-available")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockSoldOut_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/simulate/stock-sold-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(stockService).simulateStockSoldOut(orderId);
    }

    @Test
    void stockSoldOut_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/simulate/stock-sold-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shipped_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/simulate/shipped")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(stockService).simulateShipped(orderId);
    }

    @Test
    void shipped_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/simulate/shipped")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliveryConfirmed_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/simulate/delivery-confirmed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(stockService).simulateDeliveryConfirmed(orderId);
    }

    @Test
    void deliveryConfirmed_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/simulate/delivery-confirmed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
