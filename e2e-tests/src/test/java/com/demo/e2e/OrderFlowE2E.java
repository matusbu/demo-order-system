package com.demo.e2e;

import com.demo.e2e.actor.Customer;
import com.demo.e2e.actor.PaymentSystem;
import com.demo.e2e.actor.Warehouse;
import com.demo.e2e.support.E2EBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.demo.e2e.support.OrderStatus.*;

@Tag("e2e")
class OrderFlowE2E extends E2EBase {

    Customer customer;
    Warehouse warehouse;
    PaymentSystem paymentSystem;

    @BeforeEach
    void setUp() {
        // Unique customer name per test ensures a dedicated WebSocket topic
        // and prevents cross-test interference from async callbacks.
        customer = new Customer("customer-" + UUID.randomUUID(), orderEngineUrl());
        warehouse = new Warehouse(stockServiceUrl());
        paymentSystem = new PaymentSystem(paymentServiceUrl());
    }

    @AfterEach
    void tearDown() {
        customer.close();
    }

    // ── Happy paths ───────────────────────────────────────────────────────────

    @Test
    void happyPath_stockFirst_orderIsDelivered() {
        customer.placesAnOrderFor("iPhone 15", 1);
        customer.mustSeeOrderInStatus(CREATED);

        warehouse.confirmsStockIsAvailable(customer.lastOrderId());
        customer.mustSeeOrderInStatus(RESERVED);

        paymentSystem.receivesPayment(customer.lastOrderId(), new BigDecimal("999.99"));
        customer.mustSeeOrderInStatus(READY_TO_SHIP);

        warehouse.confirmsOrderHasBeenShipped(customer.lastOrderId());
        customer.mustSeeOrderInStatus(SHIPPING);

        warehouse.confirmsDelivery(customer.lastOrderId());
        customer.mustSeeOrderInStatus(DELIVERED);
    }

    @Test
    void happyPath_paymentFirst_orderIsDelivered() {
        customer.placesAnOrderFor("iPhone 15", 1);
        customer.mustSeeOrderInStatus(CREATED);

        paymentSystem.receivesPayment(customer.lastOrderId(), new BigDecimal("999.99"));
        customer.mustSeeOrderInStatus(PAID);

        warehouse.confirmsStockIsAvailable(customer.lastOrderId());
        customer.mustSeeOrderInStatus(READY_TO_SHIP);

        warehouse.confirmsOrderHasBeenShipped(customer.lastOrderId());
        customer.mustSeeOrderInStatus(SHIPPING);

        warehouse.confirmsDelivery(customer.lastOrderId());
        customer.mustSeeOrderInStatus(DELIVERED);
    }

    // ── Cancellation flows ────────────────────────────────────────────────────

    @Test
    void earlyCancellation_fromCreated_orderIsCancelled() {
        customer.placesAnOrderFor("iPhone 15", 1);
        customer.mustSeeOrderInStatus(CREATED);

        customer.cancelsOrder();
        customer.mustSeeOrderInStatus(CANCELLED);
    }

    @Test
    void cancellation_afterStockReserved_stockIsReleasedAndOrderIsCancelled() {
        customer.placesAnOrderFor("iPhone 15", 1);
        customer.mustSeeOrderInStatus(CREATED);

        warehouse.confirmsStockIsAvailable(customer.lastOrderId());
        customer.mustSeeOrderInStatus(RESERVED);

        customer.cancelsOrder();
        customer.mustSeeOrderInStatus(RELEASING_RESERVATION);
        // stock-service fires RESERVATION_CANCELLED webhook after ~2 s
        customer.mustSeeOrderInStatus(CANCELLED);
    }

    // ── Compensation flows ────────────────────────────────────────────────────

    @Test
    void stockSoldOut_afterPayment_paymentIsReturnedAndOrderIsCancelled() {
        customer.placesAnOrderFor("iPhone 15", 1);
        customer.mustSeeOrderInStatus(CREATED);

        paymentSystem.receivesPayment(customer.lastOrderId(), new BigDecimal("999.99"));
        customer.mustSeeOrderInStatus(PAID);

        warehouse.confirmsStockIsSoldOut(customer.lastOrderId());
        customer.mustSeeOrderInStatus(RETURNING_PAYMENT);
        // payment-service fires PAYMENT_RETURNED webhook after ~3 s
        customer.mustSeeOrderInStatus(CANCELLED);
    }
}
