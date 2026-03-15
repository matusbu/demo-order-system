package com.demo.stockservice.controller;

import com.demo.stockservice.dto.SimulateRequest;
import com.demo.stockservice.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
public class SimulateController {

    private final StockService stockService;

    @PostMapping("/stock-available")
    public void stockAvailable(@Valid @RequestBody SimulateRequest request) {
        stockService.simulateStockAvailable(request.orderId());
    }

    @PostMapping("/stock-sold-out")
    public void stockSoldOut(@Valid @RequestBody SimulateRequest request) {
        stockService.simulateStockSoldOut(request.orderId());
    }

    @PostMapping("/shipped")
    public void shipped(@Valid @RequestBody SimulateRequest request) {
        stockService.simulateShipped(request.orderId());
    }

    @PostMapping("/delivery-confirmed")
    public void deliveryConfirmed(@Valid @RequestBody SimulateRequest request) {
        stockService.simulateDeliveryConfirmed(request.orderId());
    }
}
