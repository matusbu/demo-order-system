package com.demo.paymentservice.service;

import com.demo.paymentservice.dto.PaymentEvent;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class PaymentTimeoutServiceIT {

    @MockitoBean
    private OrderEngineClient orderEngineClient;

    @Autowired
    private PaymentTimeoutService paymentTimeoutService;

    @Test
    void register_andProcessPayment_sendsPaymentReceivedEvent() {
        UUID orderId = UUID.randomUUID();

        paymentTimeoutService.register(orderId);
        paymentTimeoutService.processPayment(orderId, BigDecimal.valueOf(99.99));

        verify(orderEngineClient, timeout(2000)).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RECEIVED);
    }

    @Test
    void register_whenTimeoutExpires_sendsPaymentTimeoutEvent() {
        UUID orderId = UUID.randomUUID();

        // payment.timeout-minutes=0 in application-test.yml → fires with 0-minute delay
        paymentTimeoutService.register(orderId);

        verify(orderEngineClient, timeout(2000)).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_TIMEOUT);
    }

    @Test
    void initiateReturn_sendsPaymentReturnedEventAfterDelay() {
        UUID orderId = UUID.randomUUID();

        // return delay is hardcoded to 3 seconds in PaymentTimeoutService
        paymentTimeoutService.initiateReturn(orderId);

        verify(orderEngineClient, timeout(5000)).sendPaymentEvent(orderId, PaymentEvent.PAYMENT_RETURNED);
    }
}
