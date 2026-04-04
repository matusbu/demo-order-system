package com.demo.stockservice.service;

import com.demo.stockservice.dto.StockEvent;
import com.demo.stockservice.integration.OrderEngineClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class StockServiceIT {

    @MockitoBean
    private OrderEngineClient orderEngineClient;

    @Autowired
    private StockService stockService;

    @Test
    void reserve_doesNotThrow() {
        stockService.reserve(UUID.randomUUID(), "MacBook Pro", 1);
        // reservation is stored in-memory; no external side effects to verify here
    }

    @Test
    void cancelReservation_sendsReservationCancelledEventAfterDelay() {
        UUID orderId = UUID.randomUUID();
        stockService.reserve(orderId, "MacBook Pro", 1);

        // simulation.cancellation-delay-seconds=1 in application-test.yml
        stockService.cancelReservation(orderId);

        verify(orderEngineClient, timeout(3000)).sendStockEvent(orderId, StockEvent.RESERVATION_CANCELLED);
    }

    @Test
    void ship_doesNotThrow() {
        UUID orderId = UUID.randomUUID();
        stockService.reserve(orderId, "MacBook Pro", 1);

        stockService.ship(orderId);
        // shipment is stored in-memory; no external side effects to verify here
    }

    @Test
    void simulateStockAvailable_sendsStockReservedEvent() {
        UUID orderId = UUID.randomUUID();

        stockService.simulateStockAvailable(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_RESERVED);
    }

    @Test
    void simulateStockSoldOut_sendsStockSoldOutEvent() {
        UUID orderId = UUID.randomUUID();

        stockService.simulateStockSoldOut(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_SOLD_OUT);
    }

    @Test
    void simulateShipped_sendsShipmentRequestedEvent() {
        UUID orderId = UUID.randomUUID();

        stockService.simulateShipped(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.SHIPMENT_REQUESTED);
    }

    @Test
    void simulateDeliveryConfirmed_sendsDeliveryConfirmedEvent() {
        UUID orderId = UUID.randomUUID();

        stockService.simulateDeliveryConfirmed(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.DELIVERY_CONFIRMED);
    }
}
