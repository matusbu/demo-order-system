package com.demo.paymentservice.service;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutService.class);

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
                    log.info("Payment timeout fired for order {}, notifying order-engine", orderId);
                    orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_TIMEOUT);
                },
                timeoutMinutes, TimeUnit.MINUTES
        );
        pendingTimeouts.put(orderId, future);
        log.info("Order {} registered for payment tracking, timeout in {}s", orderId, timeoutMinutes * 60);
    }

    public void cancel(UUID orderId) {
        ScheduledFuture<?> future = pendingTimeouts.remove(orderId);
        if (future != null) {
            future.cancel(false);
            log.info("Payment timeout countdown cancelled for order {}", orderId);
        }
    }

    public void initiateReturn(UUID orderId) {
        log.info("Payment return initiated for order {}", orderId);
        scheduler.schedule(
                () -> {
                    log.info("Payment return confirmed for order {}, notifying order-engine", orderId);
                    orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RETURNED);
                },
                3, TimeUnit.SECONDS
        );
    }

    public void processPayment(UUID orderId, BigDecimal amount) {
        log.info("Payment received for order {}, amount: {}, notifying order-engine", orderId, amount);
        cancel(orderId);
        orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
    }

    public void simulateTimeout(UUID orderId) {
        cancel(orderId);
        log.info("Payment timeout simulated for order {}, notifying order-engine", orderId);
        orderEngineClient.sendPaymentEvent(orderId, PaymentEvent.PAYMENT_TIMEOUT);
    }
}
