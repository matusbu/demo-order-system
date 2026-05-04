package com.demo.e2e.tasks;

import com.demo.e2e.interactions.CallSimulateEndpoint;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class SimulateShipped implements Task {

    private final String orderId;

    public SimulateShipped(String orderId) {
        this.orderId = orderId;
    }

    public static Performable forOrder(String orderId) {
        return instrumented(SimulateShipped.class, orderId);
    }

    @Override
    @Step("{0} simulates shipment for order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                CallSimulateEndpoint.at("/simulate/shipped").withOrderId(orderId)
        );
    }
}
