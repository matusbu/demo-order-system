package com.demo.e2e.tasks;

import com.demo.e2e.interactions.CallSimulateEndpoint;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class SimulateStockSoldOut implements Task {

    private final String orderId;

    public SimulateStockSoldOut(String orderId) {
        this.orderId = orderId;
    }

    public static Performable forOrder(String orderId) {
        return instrumented(SimulateStockSoldOut.class, orderId);
    }

    @Override
    @Step("{0} simulates stock sold out for order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                CallSimulateEndpoint.at("/simulate/stock-sold-out").withOrderId(orderId)
        );
    }
}
