package com.demo.e2e;

import com.demo.e2e.actor.Customer;
import com.demo.e2e.actor.PaymentSystem;
import com.demo.e2e.actor.Warehouse;
import com.demo.e2e.questions.TheOrderStatus;
import com.demo.e2e.support.E2EBase;
import com.demo.e2e.tasks.*;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.ensure.Ensure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

class OrderFlowE2E extends E2EBase {

    private Actor alice;
    private Actor warehouse;
    private Actor paymentSystem;

    @BeforeEach
    void setUpActors() {
        alice         = Customer.named("Alice",         "http://localhost:" + orderEnginePort);
        warehouse     = Warehouse.named("Warehouse",    "http://localhost:" + stockServicePort);
        paymentSystem = PaymentSystem.named("PaymentSystem", "http://localhost:" + paymentServicePort);
    }

    @Test
    void happyPath_orderIsDelivered() {
        alice.attemptsTo(PlaceAnOrder.with("Alice", "iPhone 16", 1));
        String orderId = alice.recall("orderId");

        warehouse.attemptsTo(SimulateStockAvailable.forOrder(orderId));
        paymentSystem.attemptsTo(SimulatePayment.forOrder(orderId));
        warehouse.attemptsTo(SimulateShipped.forOrder(orderId));
        warehouse.attemptsTo(SimulateDeliveryConfirmed.forOrder(orderId));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                alice.attemptsTo(Ensure.that(TheOrderStatus.forOrderWithId(orderId)).isEqualTo("DELIVERED"))
        );
    }

    @Test
    void earlyCancellation_fromCreated_orderIsCancelled() {
        alice.attemptsTo(PlaceAnOrder.with("Alice", "MacBook Pro", 1));
        String orderId = alice.recall("orderId");

        alice.attemptsTo(CancelAnOrder.withId(orderId));

        alice.attemptsTo(Ensure.that(TheOrderStatus.forOrderWithId(orderId)).isEqualTo("CANCELLED"));
    }

    @Test
    void stockSoldOut_afterCreated_orderIsCancelled() {
        alice.attemptsTo(PlaceAnOrder.with("Alice", "AirPods Pro", 2));
        String orderId = alice.recall("orderId");

        warehouse.attemptsTo(SimulateStockSoldOut.forOrder(orderId));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                alice.attemptsTo(Ensure.that(TheOrderStatus.forOrderWithId(orderId)).isEqualTo("CANCELLED"))
        );
    }
}
