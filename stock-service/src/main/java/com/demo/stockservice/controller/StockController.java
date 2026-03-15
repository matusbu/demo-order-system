package com.demo.stockservice.controller;

import com.demo.stockservice.dto.ReserveOrderRequest;
import com.demo.stockservice.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/reserve")
    public void reserve(@Valid @RequestBody ReserveOrderRequest request) {
        stockService.reserve(request.orderId(), request.productName(), request.quantity());
    }

    @PostMapping("/{orderId}/cancel-reservation")
    public void cancelReservation(@PathVariable UUID orderId) {
        stockService.cancelReservation(orderId);
    }

    @PostMapping("/{orderId}/ship")
    public void ship(@PathVariable UUID orderId) {
        stockService.ship(orderId);
    }
}
