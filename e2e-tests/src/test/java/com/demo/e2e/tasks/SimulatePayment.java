package com.demo.e2e.tasks;

import com.demo.e2e.interactions.CallSimulateEndpoint;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class SimulatePayment implements Task {

    private final String orderId;
    private final String amount;

    public SimulatePayment(String orderId, String amount) {
        this.orderId = orderId;
        this.amount  = amount;
    }

    public static Performable forOrder(String orderId) {
        return instrumented(SimulatePayment.class, orderId, "10.00");
    }

    @Override
    @Step("{0} simulates payment of #amount for order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                CallSimulateEndpoint.at("/simulate/payment")
                        .withOrderId(orderId)
                        .andExtraFields(",\"amount\":" + amount)
        );
    }
}
