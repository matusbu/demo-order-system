package com.demo.orderengine.controller;

import com.demo.orderengine.domain.Order;
import com.demo.orderengine.dto.PaymentWebhookRequest;
import com.demo.orderengine.dto.StockWebhookRequest;
import com.demo.orderengine.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final OrderService orderService;

    @PostMapping("/stock")
    public Order handleStock(@Valid @RequestBody StockWebhookRequest request) {
        return orderService.handleStockWebhook(request);
    }

    @PostMapping("/payment")
    public Order handlePayment(@Valid @RequestBody PaymentWebhookRequest request) {
        return orderService.handlePaymentWebhook(request);
    }
}
