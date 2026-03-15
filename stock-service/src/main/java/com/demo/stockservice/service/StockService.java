package com.demo.stockservice.service;

import com.demo.stockservice.dto.StockEvent;
import com.demo.stockservice.integration.OrderEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

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
        log.info("Stock reservation request received for order {}, product: {}, quantity: {}", orderId, productName, quantity);
        reservations.put(orderId, new ReservationEntry(orderId, productName, quantity));
    }

    public void cancelReservation(UUID orderId) {
        log.info("Reservation cancellation received for order {}", orderId);
        reservations.remove(orderId);
        scheduler.schedule(
                () -> {
                    log.info("Reservation cancelled event sent to order-engine for order {}", orderId);
                    orderEngineClient.sendStockEvent(orderId, StockEvent.RESERVATION_CANCELLED);
                },
                cancellationDelaySeconds, TimeUnit.SECONDS
        );
    }

    public void ship(UUID orderId) {
        log.info("Shipment initiated for order {}", orderId);
        shipments.put(orderId, orderId);
    }

    public void simulateStockAvailable(UUID orderId) {
        log.info("Stock reserved for order {}, notifying order-engine", orderId);
        orderEngineClient.sendStockEvent(orderId, StockEvent.STOCK_RESERVED);
    }

    public void simulateStockSoldOut(UUID orderId) {
        log.info("Stock sold out for order {}, notifying order-engine", orderId);
        orderEngineClient.sendStockEvent(orderId, StockEvent.STOCK_SOLD_OUT);
    }

    public void simulateShipped(UUID orderId) {
        log.info("Shipped event sent to order-engine for order {}", orderId);
        orderEngineClient.sendStockEvent(orderId, StockEvent.SHIPMENT_REQUESTED);
    }

    public void simulateDeliveryConfirmed(UUID orderId) {
        log.info("Delivery confirmed event sent to order-engine for order {}", orderId);
        orderEngineClient.sendStockEvent(orderId, StockEvent.DELIVERY_CONFIRMED);
    }

    private record ReservationEntry(UUID orderId, String productName, Integer quantity) {}
}
