package com.demo.paymentservice.controller;

import com.demo.paymentservice.dto.RegisterOrderRequest;
import com.demo.paymentservice.dto.SimulatePaymentRequest;
import com.demo.paymentservice.service.PaymentTimeoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentTimeoutService paymentTimeoutService;

    @PostMapping("/orders")
    public void registerOrder(@Valid @RequestBody RegisterOrderRequest request) {
        paymentTimeoutService.register(request.orderId());
    }

    @PostMapping("/orders/{orderId}/cancel")
    public void cancelOrder(@PathVariable UUID orderId) {
        paymentTimeoutService.cancel(orderId);
    }

    @PostMapping("/orders/{orderId}/return")
    public void returnOrder(@PathVariable UUID orderId) {
        paymentTimeoutService.initiateReturn(orderId);
    }

    @PostMapping("/simulate/payment")
    public void simulatePayment(@Valid @RequestBody SimulatePaymentRequest request) {
        paymentTimeoutService.processPayment(request.orderId(), request.amount());
    }
}
