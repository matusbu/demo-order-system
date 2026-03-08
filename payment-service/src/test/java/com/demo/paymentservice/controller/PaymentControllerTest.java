package com.demo.paymentservice.controller;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.service.PaymentTimeoutService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentTimeoutService paymentTimeoutService;

    @Test
    void simulatePayment_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/simulate/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s", "amount": 49.99}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(paymentTimeoutService).processPayment(eq(orderId), any());
    }

    @Test
    void simulatePayment_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/simulate/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 49.99}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulatePayment_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/simulate/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s", "amount": -1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerOrder_validRequest_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "%s"}
                                """.formatted(orderId)))
                .andExpect(status().isOk());

        verify(paymentTimeoutService).register(orderId);
    }

    @Test
    void cancelOrder_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk());

        verify(paymentTimeoutService).cancel(orderId);
    }

    @Test
    void returnOrder_returns200AndDelegatestoService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/orders/{orderId}/return", orderId))
                .andExpect(status().isOk());

        verify(paymentTimeoutService).initiateReturn(orderId);
    }
}
