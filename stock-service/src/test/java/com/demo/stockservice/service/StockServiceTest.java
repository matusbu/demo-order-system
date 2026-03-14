package com.demo.stockservice.service;

import com.demo.stockservice.dto.StockEvent;
import com.demo.stockservice.integration.OrderEngineClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private OrderEngineClient orderEngineClient;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(scheduler, orderEngineClient, 2L);
    }

    @Test
    void simulateStockAvailable_sendsStockReservedEvent() {
        UUID orderId = UUID.randomUUID();

        service.simulateStockAvailable(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_RESERVED);
    }

    @Test
    void simulateStockSoldOut_sendsStockSoldOutEvent() {
        UUID orderId = UUID.randomUUID();

        service.simulateStockSoldOut(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_SOLD_OUT);
    }

    @Test
    void simulateShipped_sendsShipmentRequestedEvent() {
        UUID orderId = UUID.randomUUID();

        service.simulateShipped(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.SHIPMENT_REQUESTED);
    }

    @Test
    void simulateDeliveryConfirmed_sendsDeliveryConfirmedEvent() {
        UUID orderId = UUID.randomUUID();

        service.simulateDeliveryConfirmed(orderId);

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.DELIVERY_CONFIRMED);
    }

    @Test
    void cancelReservation_schedulesDelayWithConfiguredSeconds() {
        UUID orderId = UUID.randomUUID();
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any());

        service.cancelReservation(orderId);

        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    @Test
    void cancelReservation_whenDelayExpires_sendsReservationCancelledEvent() {
        UUID orderId = UUID.randomUUID();
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(scheduler).schedule(runnableCaptor.capture(), anyLong(), any());

        service.cancelReservation(orderId);
        runnableCaptor.getValue().run();

        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.RESERVATION_CANCELLED);
    }
}
