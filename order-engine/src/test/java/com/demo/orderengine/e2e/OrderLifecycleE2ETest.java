package com.demo.orderengine.e2e;

import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * End-to-end tests covering the full order lifecycle across all three services.
 *
 * All services run as real Docker containers (see docker-compose.e2e.yml).
 * No mocking — inter-service HTTP communication is real.
 *
 * Tests are written using three actors that represent real-world participants:
 *   - Client         → the end user placing orders via order-engine REST API
 *   - PaymentSystem  → the external payment provider (via payment-service simulate endpoints)
 *   - WarehouseSystem→ the external warehouse & courier (via stock-service simulate endpoints)
 *
 * client.seesOrderInStatus() is used both as a synchronisation point between steps
 * and as the final assertion. It polls up to 15 seconds before failing.
 *
 * Run with: mvn test -Dgroups=e2e -pl order-engine
 */
@Tag("e2e")
class OrderLifecycleE2ETest extends E2ETestSetup {

    @Test
    void customerReceivesOrderAfterSuccessfulPaymentAndShipping() {
        UUID orderId = client.placesOrder("Alice", "MacBook Pro", 1);

        warehouseSystem.confirmsStockAvailable(orderId);
        paymentSystem.receivesPayment(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.READY_TO_SHIP);

        warehouseSystem.shipsOrder(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.SHIPPING);

        warehouseSystem.confirmsDelivery(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.DELIVERED);
    }

    @Test
    void orderIsCancelledWhenPaymentTimesOut() {
        UUID orderId = client.placesOrder("Bob", "iPhone 15", 1);

        warehouseSystem.confirmsStockAvailable(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.RESERVED);

        paymentSystem.timesOutPayment(orderId);
        // order-engine calls stock-service to cancel the reservation;
        // stock-service sends RESERVATION_CANCELLED after 1 s (SIMULATION_CANCELLATION_DELAY_SECONDS=1)
        client.seesOrderInStatus(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void orderIsRefundedWhenStockRunsOut() {
        UUID orderId = client.placesOrder("Charlie", "iPad Pro", 2);

        paymentSystem.receivesPayment(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.PAID);

        warehouseSystem.reportsSoldOut(orderId);
        // order-engine calls payment-service to return the payment;
        // payment-service sends PAYMENT_RETURNED after 3 s (hardcoded in PaymentTimeoutService)
        client.seesOrderInStatus(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void customerCanCancelWhileWaitingForStock() {
        UUID orderId = client.placesOrder("Diana", "MacBook Air", 1);

        warehouseSystem.confirmsStockAvailable(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.RESERVED);

        client.cancelsOrder(orderId);
        // order-engine calls stock-service to cancel the reservation;
        // stock-service sends RESERVATION_CANCELLED after 1 s
        client.seesOrderInStatus(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void customerCanCancelAfterPayment() {
        UUID orderId = client.placesOrder("Eve", "AirPods Pro", 1);

        paymentSystem.receivesPayment(orderId);
        client.seesOrderInStatus(orderId, OrderStatus.PAID);

        client.cancelsOrder(orderId);
        // order-engine calls payment-service to return the payment;
        // payment-service sends PAYMENT_RETURNED after 3 s
        client.seesOrderInStatus(orderId, OrderStatus.CANCELLED);
    }
}
