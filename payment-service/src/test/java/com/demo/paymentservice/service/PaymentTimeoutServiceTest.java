package com.demo.paymentservice.service;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PaymentTimeoutServiceTest {

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private OrderEngineClient orderEngineClient;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private PaymentTimeoutService service;

    @BeforeEach
    void setUp() {
        service = new PaymentTimeoutService(scheduler, orderEngineClient, 5L);
    }

    @Test
    void register_schedulesTimeoutWithConfiguredDelay() {
        UUID orderId = UUID.randomUUID();
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any());

        service.register(orderId);

        verify(scheduler).schedule(any(Runnable.class), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void register_whenTimeoutFires_sendsPaymentTimeoutEvent() {
        UUID orderId = UUID.randomUUID();
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(scheduler).schedule(runnableCaptor.capture(), anyLong(), any());

        service.register(orderId);
        runnableCaptor.getValue().run();

        verify(orderEngineClient).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_TIMEOUT);
    }

    @Test
    void cancel_cancelsScheduledFuture() {
        UUID orderId = UUID.randomUUID();
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any());
        service.register(orderId);

        service.cancel(orderId);

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void cancel_unknownOrder_doesNothing() {
        service.cancel(UUID.randomUUID());
        verifyNoInteractions(scheduledFuture, orderEngineClient);
    }

    @Test
    void initiateReturn_schedulesThreeSecondDelay() {
        UUID orderId = UUID.randomUUID();

        service.initiateReturn(orderId);

        verify(scheduler).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
    }

    @Test
    void initiateReturn_whenDelayExpires_sendsPaymentReturnedEvent() {
        UUID orderId = UUID.randomUUID();
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(scheduler).schedule(runnableCaptor.capture(), anyLong(), any());

        service.initiateReturn(orderId);
        runnableCaptor.getValue().run();

        verify(orderEngineClient).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RETURNED);
    }

    @Test
    void processPayment_cancelsTimeoutAndSendsPaymentReceivedEvent() {
        UUID orderId = UUID.randomUUID();
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any());
        service.register(orderId);

        service.processPayment(orderId, new BigDecimal("49.99"));

        verify(scheduledFuture).cancel(false);
        verify(orderEngineClient).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
    }

    @Test
    void processPayment_withoutPriorRegister_stillSendsPaymentReceivedEvent() {
        UUID orderId = UUID.randomUUID();

        service.processPayment(orderId, new BigDecimal("99.00"));

        verify(orderEngineClient).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
        verifyNoInteractions(scheduledFuture);
    }
}
