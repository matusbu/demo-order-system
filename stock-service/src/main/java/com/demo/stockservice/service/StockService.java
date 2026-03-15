package com.demo.stockservice.service;

import com.demo.stockservice.dto.StockEvent;
import com.demo.stockservice.integration.OrderEngineClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    private final ConcurrentHashMap<UUID, ReservationEntry> reservations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> shipments = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler;
    private final OrderEngineClient orderEngineClient;
    private final long cancellationDelaySeconds;

    public StockService(ScheduledExecutorService scheduler,
                        OrderEngineClient orderEngineClient,
                        @Value("${simulation.cancellation-delay-seconds}") long cancellationDelaySeconds) {
        this.scheduler = scheduler;
        this.orderEngineClient = orderEngineClient;
        this.cancellationDelaySeconds = cancellationDelaySeconds;
    }

    public void reserve(UUID orderId, String productName, Integer quantity) {
        reservations.put(orderId, new ReservationEntry(orderId, productName, quantity));
    }

    public void cancelReservation(UUID orderId) {
        reservations.remove(orderId);
        scheduler.schedule(
                () -> orderEngineClient.sendStockEvent(orderId, StockEvent.RESERVATION_CANCELLED),
                cancellationDelaySeconds, TimeUnit.SECONDS
        );
    }

    public void ship(UUID orderId) {
        shipments.put(orderId, orderId);
    }

    public void simulateStockAvailable(UUID orderId) {
        orderEngineClient.sendStockEvent(orderId, StockEvent.STOCK_RESERVED);
    }

    public void simulateStockSoldOut(UUID orderId) {
        orderEngineClient.sendStockEvent(orderId, StockEvent.STOCK_SOLD_OUT);
    }

    public void simulateShipped(UUID orderId) {
        orderEngineClient.sendStockEvent(orderId, StockEvent.SHIPMENT_REQUESTED);
    }

    public void simulateDeliveryConfirmed(UUID orderId) {
        orderEngineClient.sendStockEvent(orderId, StockEvent.DELIVERY_CONFIRMED);
    }

    private record ReservationEntry(UUID orderId, String productName, Integer quantity) {}
}
