package com.demo.paymentservice.service;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentTimeoutService {

    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingTimeouts = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler;
    private final OrderEngineClient orderEngineClient;
    private final long timeoutMinutes;

    public PaymentTimeoutService(ScheduledExecutorService scheduler,
                                 OrderEngineClient orderEngineClient,
                                 @Value("${payment.timeout-minutes}") long timeoutMinutes) {
        this.scheduler = scheduler;
        this.orderEngineClient = orderEngineClient;
        this.timeoutMinutes = timeoutMinutes;
    }

    public void register(UUID orderId) {
        ScheduledFuture<?> future = scheduler.schedule(
                () -> {
                    pendingTimeouts.remove(orderId);
                    orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_TIMEOUT);
                },
                timeoutMinutes, TimeUnit.MINUTES
        );
        pendingTimeouts.put(orderId, future);
    }

    public void cancel(UUID orderId) {
        ScheduledFuture<?> future = pendingTimeouts.remove(orderId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void initiateReturn(UUID orderId) {
        scheduler.schedule(
                () -> orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RETURNED),
                3, TimeUnit.SECONDS
        );
    }

    public void processPayment(UUID orderId, BigDecimal amount) {
        cancel(orderId);
        orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
    }
}
