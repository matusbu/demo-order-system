package com.demo.e2e.interactions;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Delete;

public class CancelOrder implements Interaction {

    private final String orderId;

    private CancelOrder(String orderId) {
        this.orderId = orderId;
    }

    public static CancelOrder withId(String orderId) {
        return new CancelOrder(orderId);
    }

    @Override
    @Step("{0} deletes order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Delete.from("/orders/" + orderId + "/cancel")
        );
    }
}
