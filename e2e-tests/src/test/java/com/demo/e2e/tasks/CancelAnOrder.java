package com.demo.e2e.tasks;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.rest.interactions.Delete;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class CancelAnOrder implements Task {

    private final String orderId;

    public CancelAnOrder(String orderId) {
        this.orderId = orderId;
    }

    public static Performable withId(String orderId) {
        return instrumented(CancelAnOrder.class, orderId);
    }

    @Override
    @Step("{0} cancels order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Delete.from("/orders/" + orderId + "/cancel")
        );
    }
}
